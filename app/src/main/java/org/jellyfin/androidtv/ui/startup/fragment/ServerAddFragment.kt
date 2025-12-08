package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.databinding.FragmentServerAddBinding
import org.jellyfin.androidtv.ui.startup.ServerAddViewModel
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.tailscale.TailscaleManager
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import timber.log.Timber

class ServerAddFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ADDRESS = "server_address"
	}

	private val startupViewModel: ServerAddViewModel by viewModel()
	private val serverRepository: ServerRepository by inject()
	private var _binding: FragmentServerAddBinding? = null
	private val binding get() = _binding!!

	private var vpnPermissionCallback: ((Boolean) -> Unit)? = null

	private val vpnPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		vpnPermissionCallback?.invoke(result.resultCode == Activity.RESULT_OK)
		vpnPermissionCallback = null
	}

	private val serverAddressArgument get() = arguments?.getString(ARG_SERVER_ADDRESS)?.ifBlank { null }
	private var currentDialog: AlertDialog? = null
	private var useTailscale = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentServerAddBinding.inflate(inflater, container, false)

		with(binding.address) {
			setOnEditorActionListener { _, actionId, _ ->
				when (actionId) {
					EditorInfo.IME_ACTION_DONE -> {
						submitAddress()
						true
					}
					else -> false
				}
			}
		}

		// "Lokal verbinden" Button
		binding.connectLocal.setOnClickListener {
			Timber.d("User selected: Local connection")
			useTailscale = false
			showAddressInput()
		}

		// "Über Tailscale VPN" Button
		binding.connectTailscale.setOnClickListener {
			Timber.d("User selected: Tailscale VPN connection")
			useTailscale = true
			// Buttons disablen + Loading-Indikator
			setButtonsEnabled(false)
			binding.connectTailscale.text = "Verbinde mit Tailscale..."
			startTailscaleFlow()
		}

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (serverAddressArgument != null) {
			binding.address.setText(serverAddressArgument)
			binding.address.isEnabled = false
			submitAddress()
		}

		startupViewModel.state.onEach { state ->
			when (state) {
				is ConnectingState -> {
					binding.address.isEnabled = false
					binding.connectLocal.isEnabled = false
					binding.connectTailscale.isEnabled = false
					binding.error.text = getString(R.string.server_connecting, state.address)
				}

				is UnableToConnectState -> {
					binding.address.isEnabled = true
					setButtonsEnabled(true)
					binding.error.text = getString(
						R.string.server_connection_failed_candidates,
						state.addressCandidates
							.map { "${it.key} - ${it.value.getSummary(requireContext())}" }
							.joinToString(prefix = "\n", separator = "\n")
					)
				}

				is ConnectedState -> parentFragmentManager.commit {
					if (useTailscale) {
						lifecycleScope.launch {
							serverRepository.setTailscaleEnabled(state.id, true)
						}
					}
					replace<StartupToolbarFragment>(R.id.content_view)
					add<ServerFragment>(
						R.id.content_view,
						null,
						bundleOf(ServerFragment.ARG_SERVER_ID to state.id.toString())
					)
				}

				null -> Unit
			}
		}.launchIn(lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		currentDialog?.dismiss()
		currentDialog = null
		_binding = null
	}

	private fun setButtonsEnabled(enabled: Boolean) {
		binding.connectLocal.isEnabled = enabled
		binding.connectTailscale.isEnabled = enabled
		if (enabled) {
			binding.connectTailscale.text = getString(R.string.connect_tailscale_button)
		}
	}

	private fun showAddressInput() {
		// Buttons ausblenden
		binding.connectionTypeLabel.isVisible = false
		binding.connectLocal.isVisible = false
		binding.connectTailscale.isVisible = false

		// Server-Adresse anzeigen
		binding.addressLabel.isVisible = true
		binding.address.isVisible = true
		binding.address.requestFocus()
	}

	/**
	 * Tailscale-Flow mit verbessertem Logging und Auto-Close
	 */
	private fun startTailscaleFlow() {
		viewLifecycleOwner.lifecycleScope.launch {
			try {
				Timber.d("=== TAILSCALE FLOW START ===")

				// 0. Sicherstellen, dass VPN-Permission erteilt ist (zeigt Systemdialog falls nötig)
				val permissionGranted = ensureVpnPermission()
				if (!permissionGranted) {
					Timber.w("VPN permission not granted by user")
					setButtonsEnabled(true)
					Toast.makeText(requireContext(), "VPN-Berechtigung erforderlich.", Toast.LENGTH_LONG).show()
					return@launch
				}

				// 1. Login-Code anfordern
				Timber.d("Step 1: Requesting login code...")
				val codeResult = TailscaleManager.requestLoginCode()
				if (codeResult.isFailure) {
					Timber.e(codeResult.exceptionOrNull(), "Failed to request login code")
					setButtonsEnabled(true)
					AlertDialog.Builder(requireContext())
						.setTitle("Fehler")
						.setMessage("Login-Code konnte nicht angefordert werden:\n\n${codeResult.exceptionOrNull()?.message}")
						.setPositiveButton("OK", null)
						.show()
					return@launch
				}

				val code = codeResult.getOrThrow()
				Timber.d("Step 1: Got login code: $code")

				// 2. Dialog mit Code (ohne Kopieren-Button)
				Timber.d("Step 2: Showing dialog with code")
				val dialogShowTime = System.currentTimeMillis()
				currentDialog = AlertDialog.Builder(requireContext())
					.setTitle(R.string.tailscale_connecting_title)
					.setMessage(getString(R.string.tailscale_connecting_message, code))
					.setCancelable(false)
					.show()

				// 3. Warte auf loginFinished Event (VPN startet automatisch nach Login!)
				Timber.d("Step 3: Waiting for loginFinished event (max 120 seconds)...")

				// Starte einen separaten Job für regelmäßiges Status-Logging
				val loggingJob = launch {
					while (true) {
						kotlinx.coroutines.delay(5000) // Alle 5 Sekunden
						val isConnected = TailscaleManager.isConnected()
						Timber.d("Connection status check: isConnected=$isConnected")
					}
				}

				val loginFinished = TailscaleManager.waitUntilLoginFinished(timeoutMs = 120_000L)
				loggingJob.cancel()

				Timber.d("Step 3: Wait completed. loginFinished=$loginFinished")

				// WICHTIG: Stelle sicher, dass der Dialog mindestens 3 Sekunden sichtbar war
				val dialogVisibleTime = System.currentTimeMillis() - dialogShowTime
				if (dialogVisibleTime < 3000L) {
					val remainingTime = 3000L - dialogVisibleTime
					Timber.d("Dialog was only visible for ${dialogVisibleTime}ms, waiting additional ${remainingTime}ms")
					kotlinx.coroutines.delay(remainingTime)
				}

				// Dialog schließen
				currentDialog?.dismiss()
				currentDialog = null

				if (loginFinished) {
					Timber.d("Step 4: Login finished successfully!")

					// WICHTIG: Nach erfolgreichem Login MUSS der VPN gestartet werden!
					// Das Tailscale SDK setzt NICHT automatisch WantRunning=true
					Timber.d("Step 4: Starting VPN after successful login...")
					val vpnStarted = TailscaleManager.startVpn()
					if (!vpnStarted) {
						Timber.w("VPN start failed - permission needed")
						setButtonsEnabled(true)
						Toast.makeText(requireContext(), "VPN-Permission benötigt. Bitte erlauben und erneut versuchen.", Toast.LENGTH_LONG).show()
						return@launch
					}

					// Warte bis VPN verbunden ist
					Timber.d("Step 5: Waiting for VPN to connect...")
					val vpnConnected = TailscaleManager.waitUntilConnected(timeoutMs = 60_000L)

					if (vpnConnected) {
						Timber.d("Step 5: VPN connected successfully!")
						AlertDialog.Builder(requireContext())
							.setTitle(R.string.tailscale_connected_title)
							.setMessage(R.string.tailscale_connected_message)
							.setPositiveButton("OK") { dialog, _ ->
								dialog.dismiss()
								showAddressInput()
							}
							.show()
					} else {
						Timber.e("Step 5: VPN connection timeout!")
						setButtonsEnabled(true)
						AlertDialog.Builder(requireContext())
							.setTitle("VPN Timeout")
							.setMessage("Tailscale Login war erfolgreich, aber VPN konnte nicht verbunden werden.\n\nPrüfe die Diagnose oder probiere es erneut.")
							.setPositiveButton("OK") { _, _ ->
								setButtonsEnabled(true)
							}
							.show()
					}
				} else {
					Timber.e("Step 4: TIMEOUT - loginFinished event never received")
					setButtonsEnabled(true)
					AlertDialog.Builder(requireContext())
						.setTitle("Login Timeout")
						.setMessage("Der Login konnte nicht abgeschlossen werden.\n\nWurde das Gerät im Tailscale-Dashboard freigeschaltet?\n\nBitte prüfe:\n- Dashboard → Machines → Gerät genehmigen")
						.setPositiveButton("Erneut versuchen") { _, _ ->
							// Erlaube erneuten Versuch
							setButtonsEnabled(true)
						}
						.setNegativeButton("Abbrechen") { _, _ ->
							setButtonsEnabled(true)
						}
						.show()
				}

				Timber.d("=== TAILSCALE FLOW END ===")
			} catch (e: Exception) {
				Timber.e(e, "Tailscale flow failed with exception")
				currentDialog?.dismiss()
				setButtonsEnabled(true)
				Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
			}
		}
	}

	/**
	 * Zeigt den systemweiten VPN-Permission-Dialog an, falls nötig, und wartet auf das Ergebnis.
	 */
	private suspend fun ensureVpnPermission(): Boolean = suspendCoroutine { cont ->
		val intent = VpnService.prepare(requireContext())
		if (intent == null) {
			cont.resume(true)
			return@suspendCoroutine
		}

		vpnPermissionCallback = { granted ->
			cont.resume(granted)
		}
		try {
			vpnPermissionLauncher.launch(intent)
		} catch (t: Throwable) {
			vpnPermissionCallback = null
			cont.resumeWithException(t)
		}
	}

	private fun submitAddress() = when {
		binding.address.text.isNotBlank() -> {
			val address = binding.address.text.toString().trim()

			// Wenn Tailscale aktiviert ist und die Adresse KEINE IP/URL ist,
			// versuche sie als Tailscale-Hostname aufzulösen
			if (useTailscale && !address.contains(".") && !address.contains(":")) {
				Timber.d("submitAddress: Address '$address' looks like a hostname, trying Tailscale resolution...")
				lifecycleScope.launch {
					try {
						val resolvedIp = TailscaleManager.getPeerIpByHostname(address)
						if (resolvedIp != null) {
							Timber.d("submitAddress: Resolved '$address' to $resolvedIp")
							// Verwende die aufgelöste IP
							startupViewModel.addServer(resolvedIp)
						} else {
							Timber.w("submitAddress: Could not resolve '$address' via Tailscale, using as-is")
							// Fallback: Verwende die Adresse wie eingegeben
							startupViewModel.addServer(address)
						}
					} catch (e: Exception) {
						Timber.e(e, "submitAddress: Tailscale resolution failed")
						// Fallback: Verwende die Adresse wie eingegeben
						startupViewModel.addServer(address)
					}
				}
			} else {
				// Normale Adresse (IP oder URL)
				startupViewModel.addServer(address)
			}
		}
		else -> binding.error.setText(R.string.server_field_empty)
	}
}
