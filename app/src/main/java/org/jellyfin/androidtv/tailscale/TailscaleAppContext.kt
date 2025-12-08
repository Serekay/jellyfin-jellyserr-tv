package org.jellyfin.androidtv.tailscale

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.Base64
import libtailscale.AppContext

/**
 * Minimal implementation of libtailscale.AppContext backed by SharedPreferences.
 * This is intentionally lightweight; features like hardware attestation are stubbed.
 */
class TailscaleAppContext(private val context: Context) : AppContext {
	private val prefs = context.getSharedPreferences("tailscale_prefs", Context.MODE_PRIVATE)

	private fun encode(value: String): String = if (Build.VERSION.SDK_INT >= 26) {
		Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
	} else {
		@Suppress("DEPRECATION")
		android.util.Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
	}

	private fun decode(value: String?): String? = when {
		value.isNullOrEmpty() -> null
		Build.VERSION.SDK_INT >= 26 -> String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
		else -> {
			@Suppress("DEPRECATION")
			String(android.util.Base64.decode(value, android.util.Base64.NO_WRAP), StandardCharsets.UTF_8)
		}
	}

	override fun decryptFromPref(key: String): String? = decode(prefs.getString(key, null))

	override fun encryptToPref(key: String, value: String) {
		prefs.edit().putString(key, encode(value)).apply()
	}

	override fun getDeviceName(): String = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
		?: Build.MODEL

	override fun getInstallSource(): String = context.packageManager.getInstallerPackageName(context.packageName)
		?: "unknown"

	override fun getInterfacesAsString(): String = try {
		NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
			.joinToString(",") { it.displayName }
	} catch (t: Throwable) {
		""
	}

	override fun getOSVersion(): String = Build.VERSION.RELEASE ?: "unknown"

	override fun getPlatformDNSConfig(): String {
		val cm = context.getSystemService<ConnectivityManager>() ?: return ""
		val active = cm.activeNetwork ?: return ""
		val linkProps = cm.getLinkProperties(active) ?: return ""
		val dns = linkProps.dnsServers ?: emptyList()
		return dns.joinToString(",") { it.hostAddress ?: "" }
	}

	override fun getStateStoreKeysJSON(): String = "[]"

	override fun getSyspolicyBooleanValue(key: String): Boolean = false

	override fun getSyspolicyStringArrayJSONValue(key: String): String = "[]"

	override fun getSyspolicyStringValue(key: String): String = ""

	override fun hardwareAttestationKeyCreate(): String = ""

	override fun hardwareAttestationKeyLoad(keyName: String) {}

	override fun hardwareAttestationKeyPublic(keyName: String): ByteArray = ByteArray(0)

	override fun hardwareAttestationKeyRelease(keyName: String) {}

	override fun hardwareAttestationKeySign(keyName: String, payload: ByteArray): ByteArray = ByteArray(0)

	override fun hardwareAttestationKeySupported(): Boolean = false

	override fun isChromeOS(): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_PC)

	override fun log(s: String, s1: String) {
		android.util.Log.d(s, s1)
	}

	override fun shouldUseGoogleDNSFallback(): Boolean = true
}
