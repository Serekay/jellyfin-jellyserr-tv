package org.jellyfin.androidtv.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
	private const val TAG = "UpdateChecker"
	private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Serekay/jellyfin-jellyserr-tv/releases/latest"
	private const val APK_NAME = "jellyarc-update.apk"

	data class ReleaseInfo(
		val version: String,
		val downloadUrl: String,
		val changelog: String?
	)

	suspend fun fetchLatestRelease(currentVersion: String): ReleaseInfo? = withContext(Dispatchers.IO) {
		try {
			val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
				requestMethod = "GET"
				connectTimeout = 8000
				readTimeout = 8000
				setRequestProperty("Accept", "application/vnd.github+json")
				setRequestProperty("User-Agent", "jellyarc-tv")
			}

			connection.inputStream.buffered().use { stream ->
				val payload = stream.reader().readText()
				val json = JSONObject(payload)
				val tagName = normalizeVersion(json.optString("tag_name"))
				val htmlUrl = json.optString("html_url")
				val apkUrl = findApkUrl(json) ?: htmlUrl
				val changelog = json.optString("body")

				if (tagName.isNotEmpty() && isNewer(tagName, normalizeVersion(currentVersion))) {
					ReleaseInfo(tagName, apkUrl, changelog)
				} else null
			}
		} catch (ex: Exception) {
			Log.w(TAG, "Update check failed", ex)
			null
		}
	}

	private fun findApkUrl(json: JSONObject): String? {
		val assets = json.optJSONArray("assets") ?: return null
		for (i in 0 until assets.length()) {
			val asset = assets.optJSONObject(i) ?: continue
			val name = asset.optString("name")
			val url = asset.optString("browser_download_url")
			if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
				return url
			}
		}
		return null
	}

	private fun normalizeVersion(version: String): String =
		version.removePrefix("v").trim()

	private fun isNewer(latest: String, current: String): Boolean {
		val latestParts = latest.split(".")
		val currentParts = current.split(".")
		val max = maxOf(latestParts.size, currentParts.size)

		for (index in 0 until max) {
			val latestPart = latestParts.getOrNull(index)?.toIntOrNull() ?: 0
			val currentPart = currentParts.getOrNull(index)?.toIntOrNull() ?: 0
			if (latestPart != currentPart) return latestPart > currentPart
		}
		return false
	}

	suspend fun notifyIfUpdateAvailable(context: Context, currentVersion: String, notifyIfCurrent: Boolean = false): ReleaseInfo? {
		val release = fetchLatestRelease(currentVersion)

		return when {
			release != null -> {
				context.showCenteredToast(context.getString(R.string.jellyseerr_update_available_toast, release.version))
				release
			}

			notifyIfCurrent -> {
				context.showCenteredToast(context.getString(R.string.jellyseerr_update_current_toast))
				null
			}

			else -> null
		}
	}

	suspend fun downloadAndInstall(context: Context, currentVersion: String, notifyIfCurrent: Boolean = false): ReleaseInfo? {
		clearOldApk(context)
		val release = notifyIfUpdateAvailable(context, currentVersion, notifyIfCurrent)
		release ?: return null

		context.showCenteredToast(context.getString(R.string.jellyseerr_update_downloading))

		val apkFile = downloadApk(context, release.downloadUrl)
		if (apkFile == null) {
			context.showCenteredToast(context.getString(R.string.jellyseerr_update_download_failed))
			return release
		}

		// Permission to install from unknown sources (Android 8.0+)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val canInstall = context.packageManager.canRequestPackageInstalls()
			if (!canInstall) {
				context.showCenteredToast(context.getString(R.string.jellyseerr_update_permission_required))
				val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
					data = Uri.parse("package:${context.packageName}")
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				}
				runCatching { context.startActivity(settingsIntent) }
					.onFailure { Log.w(TAG, "Failed to open unknown sources settings", it) }
				return release
			}
		}

		context.showCenteredToast(context.getString(R.string.jellyseerr_update_launch_installer))
		val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
		val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
			setDataAndType(uri, "application/vnd.android.package-archive")
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
		}

		runCatching { context.startActivity(intent) }
			.onFailure {
				Log.w(TAG, "Failed to launch installer", it)
				context.showCenteredToast(context.getString(R.string.jellyseerr_update_install_failed))
			}

		return release
	}

	private suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
		return@withContext try {
			val target = File(context.cacheDir, APK_NAME)
			if (target.exists()) target.delete()
			val connection = (URL(url).openConnection() as HttpURLConnection).apply {
				requestMethod = "GET"
				connectTimeout = 10000
				readTimeout = 20000
			}

			connection.inputStream.use { input ->
				target.outputStream().use { output ->
					copyStream(input, output)
				}
			}
			target
		} catch (ex: Exception) {
			Log.w(TAG, "Download failed", ex)
			null
		}
	}

	private fun clearOldApk(context: Context) {
		runCatching {
			val target = File(context.cacheDir, APK_NAME)
			if (target.exists()) target.delete()
		}
	}

	private fun copyStream(input: InputStream, output: OutputStream, bufferSize: Int = 8 * 1024) {
		val buffer = ByteArray(bufferSize)
		while (true) {
			val read = input.read(buffer)
			if (read <= 0) break
			output.write(buffer, 0, read)
		}
	}

	private fun Context.showCenteredToast(message: String) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).apply {
			setGravity(Gravity.CENTER, 0, 0)
		}.show()
	}
}
