package org.jellyfin.androidtv.ui.jellyseerr

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.sdk.TrailerUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.net.URLEncoder

/**
 * Mirrors Jellyfin's native trailer playback flow for Jellyseerr items.
 */
suspend fun playJellyseerrTrailer(
	context: Context,
	apiClient: ApiClient,
	playbackLauncher: PlaybackLauncher,
	item: JellyseerrSearchItem,
	searchTitle: String,
) {
	val trimmedSearchTitle = searchTitle.trim()

	val jellyfinId = item.jellyfinId?.takeIf { it.isNotBlank() }
	if (jellyfinId == null) {
		launchYouTubeSearch(context, trimmedSearchTitle)
		return
	}

	val uuid = jellyfinId.toUUIDOrNull()
	if (uuid == null) {
		Toast.makeText(
			context,
			context.getString(R.string.jellyseerr_trailer_error),
			Toast.LENGTH_LONG
		).show()
		launchYouTubeSearch(context, trimmedSearchTitle)
		return
	}

	val baseItem = runCatching {
		withContext(Dispatchers.IO) {
			apiClient.userLibraryApi.getItem(uuid).content
		}
	}.getOrNull()

	if (baseItem == null) {
		Toast.makeText(
			context,
			context.getString(R.string.jellyseerr_trailer_error),
			Toast.LENGTH_LONG
		).show()
		launchYouTubeSearch(context, trimmedSearchTitle)
		return
	}

	val trailersResult = runCatching {
		withContext(Dispatchers.IO) {
			apiClient.userLibraryApi.getLocalTrailers(itemId = uuid).content
		}
	}.getOrNull()

	if (!trailersResult.isNullOrEmpty()) {
		playbackLauncher.launch(context, trailersResult, position = 0)
		return
	}

	val externalIntent = TrailerUtils.getExternalTrailerIntent(context, baseItem)
	if (externalIntent != null) {
		try {
			context.startActivity(externalIntent)
			return
		} catch (exception: ActivityNotFoundException) {
			Toast.makeText(
				context,
				context.getString(R.string.no_player_message),
				Toast.LENGTH_LONG
			).show()
		}
	}

	launchYouTubeSearch(context, trimmedSearchTitle)
}

private fun launchYouTubeSearch(context: Context, title: String) {
	val trimmedTitle = title.trim()
	if (trimmedTitle.isBlank()) {
		Toast.makeText(
			context,
			context.getString(R.string.jellyseerr_trailer_unavailable),
			Toast.LENGTH_LONG
		).show()
		return
	}

	val query = URLEncoder.encode("$trimmedTitle trailer", Charsets.UTF_8.name())
	val searchIntent = Intent(
		Intent.ACTION_VIEW,
		Uri.parse("https://www.youtube.com/results?search_query=$query")
	)

	try {
		context.startActivity(searchIntent)
	} catch (exception: ActivityNotFoundException) {
		Toast.makeText(
			context,
			context.getString(R.string.no_player_message),
			Toast.LENGTH_LONG
		).show()
	}
}
