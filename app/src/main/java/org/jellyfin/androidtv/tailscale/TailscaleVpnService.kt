package org.jellyfin.androidtv.tailscale

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.system.OsConstants
import org.jellyfin.androidtv.ui.startup.StartupActivity
import libtailscale.IPNService
import libtailscale.Libtailscale
import libtailscale.VPNServiceBuilder as GoVpnServiceBuilder

class TailscaleVpnService : VpnService(), IPNService {
	private val id: String = java.util.UUID.randomUUID().toString()

	override fun id(): String = id

	override fun updateVpnStatus(status: Boolean) {
		// Could hook into UI state; not needed for minimal flow.
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_START_VPN -> {
				Libtailscale.requestVPN(this)
				return START_STICKY
			}
			ACTION_STOP_VPN -> {
				close()
				return START_NOT_STICKY
			}
		}
		return START_STICKY
	}

	override fun close() {
		Libtailscale.serviceDisconnect(this)
		stopSelf()
	}

	override fun protect(socket: Int): Boolean = super.protect(socket)

	override fun disconnectVPN() {
		stopSelf()
	}

	override fun onRevoke() {
		close()
		super.onRevoke()
	}

	override fun newBuilder(): GoVpnServiceBuilder {
		val builder = Builder()
			.setConfigureIntent(configIntent())
			.allowFamily(OsConstants.AF_INET)
			.allowFamily(OsConstants.AF_INET6)
			.setSession("Tailscale")
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			builder.setMetered(false)
		}
		builder.setUnderlyingNetworks(null)
		return TailscaleVpnServiceBuilder(builder)
	}

	private fun configIntent(): PendingIntent {
		val openIntent = Intent(this, StartupActivity::class.java)
		return PendingIntent.getActivity(
			this,
			0,
			openIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}

	companion object {
		const val ACTION_START_VPN = "org.jellyfin.androidtv.tailscale.START_VPN"
		const val ACTION_STOP_VPN = "org.jellyfin.androidtv.tailscale.STOP_VPN"
	}
}
