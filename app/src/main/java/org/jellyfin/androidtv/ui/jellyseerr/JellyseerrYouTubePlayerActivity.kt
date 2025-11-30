package org.jellyfin.androidtv.ui.jellyseerr

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import java.net.URLEncoder

class JellyseerrYouTubePlayerActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val rawUrl = intent?.getStringExtra(EXTRA_URL).orEmpty()
		val fallbackQuery = intent?.getStringExtra(EXTRA_FALLBACK_QUERY)
		if (rawUrl.isBlank()) {
			launchFallbackSearch(fallbackQuery)
			finish()
			return
		}

		setContent {
			JellyfinTheme {
				JellyseerrYouTubePlayer(rawUrl) {
					launchFallbackSearch(fallbackQuery)
					finish()
				}
			}
		}
	}

	companion object {
		private const val EXTRA_URL = "jellyseerr.youtube.url"
		private const val EXTRA_FALLBACK_QUERY = "jellyseerr.youtube.query"

		fun launch(context: Context, url: String, fallbackQuery: String? = null) {
			val intent = Intent(context, JellyseerrYouTubePlayerActivity::class.java)
				.putExtra(EXTRA_URL, url)
				.putExtra(EXTRA_FALLBACK_QUERY, fallbackQuery)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			context.startActivity(intent)
		}
	}

	private fun launchFallbackSearch(query: String?) {
		val trimmed = query?.trim().orEmpty()
		if (trimmed.isBlank()) return

		val encoded = runCatching { URLEncoder.encode(trimmed, Charsets.UTF_8.name()) }.getOrElse { return }
		val searchUrl = "https://www.youtube.com/results?search_query=$encoded"
		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		startActivity(intent)
	}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun JellyseerrYouTubePlayer(url: String, onError: () -> Unit) {
	val isLoaded = remember { mutableStateOf(false) }
	val playerHtml = remember(url) { buildPlayerHtml(url) }

	Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
		AndroidView(
			modifier = Modifier.fillMaxSize(),
			factory = { context ->
				WebView(context).apply {
					settings.javaScriptEnabled = true
					settings.mediaPlaybackRequiresUserGesture = false
					settings.domStorageEnabled = true
					settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
					setBackgroundColor(android.graphics.Color.BLACK)
					webViewClient = object : android.webkit.WebViewClient() {
						override fun onPageFinished(view: WebView?, url: String?) {
							isLoaded.value = true
						}

						override fun onReceivedError(
							view: WebView?,
							errorCode: Int,
							description: String?,
							failingUrl: String?
						) {
							onError()
						}
					}
					loadDataWithBaseURL(
						"https://www.youtube.com",
						playerHtml,
						"text/html",
						"UTF-8",
						null,
					)
				}
			},
		)

		if (!isLoaded.value) {
			CircularProgressIndicator(
				modifier = Modifier.align(Alignment.Center),
				color = Color.White,
			)
		}
	}
}

private fun buildPlayerHtml(url: String): String {
	val uri = runCatching { Uri.parse(url) }.getOrNull()
	val embedUrl = normalizeTrailerUrl(uri, url)

	val html = """
		<!DOCTYPE html>
		<html>
		<head>
			<meta name="viewport" content="width=device-width, initial-scale=1.0">
			<style>
				html, body {
					margin: 0;
					padding: 0;
					background-color: #000;
					overflow: hidden;
					width: 100%;
					height: 100%;
				}
				.player {
					position: fixed;
					top: 0;
					left: 0;
					width: 100%;
					height: 100%;
					border: none;
					background: #000;
				}
				video {
					width: 100%;
					height: 100%;
					object-fit: contain;
					background: #000;
				}
			</style>
		</head>
		<body>
			${buildPlayerBody(embedUrl)}
		</body>
		</html>
	""".trimIndent()

	return html
}

private fun buildPlayerBody(embedUrl: String): String {
	return if (embedUrl.contains("youtube.com/embed")) {
		"""<iframe class="player" src="$embedUrl" allow="autoplay; encrypted-media" allowfullscreen></iframe>"""
	} else if (embedUrl.contains("imdb.com/video/imdb")) {
		"""<iframe class="player" src="$embedUrl" allow="autoplay; fullscreen" allowfullscreen></iframe>"""
	} else if (embedUrl.endsWith(".mp4", ignoreCase = true) || embedUrl.endsWith(".mkv", ignoreCase = true)) {
		"""<video class="player" src="$embedUrl" controls autoplay></video>"""
	} else {
		"""<iframe class="player" src="$embedUrl" allowfullscreen></iframe>"""
	}
}

private fun normalizeTrailerUrl(uri: Uri?, fallback: String): String {
	if (uri == null) return fallback
	val host = uri.host.orEmpty().lowercase()

	// YouTube watch or short links
	if (host.contains("youtube.com") || host.contains("youtu.be")) {
		val vid = uri.getQueryParameter("v")
			?: uri.lastPathSegment
		if (!vid.isNullOrBlank() && vid.length >= 8) {
			return "https://www.youtube.com/embed/$vid?autoplay=1&controls=1&fs=1&playsinline=1"
		}
	}

	// IMDb video URLs like /video/vi123456789/
	if (host.contains("imdb.com")) {
		val segments = uri.pathSegments
		val vi = segments.firstOrNull { it.startsWith("vi") }
		if (vi != null) {
			return "https://www.imdb.com/video/imdb/$vi/imdb/embed?autoplay=true&width=1280"
		}
	}

	return fallback
}
