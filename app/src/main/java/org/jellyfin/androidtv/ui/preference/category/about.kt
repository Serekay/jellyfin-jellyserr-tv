package org.jellyfin.androidtv.ui.preference.category

import android.os.Build
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.screen.LicensesScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.util.UpdateChecker

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	link {
		// Hardcoded strings for troubleshooting purposes
		title = "JellyArc app version"
		content = "jellyarc-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"
		icon = R.drawable.ic_jellyfin
	}

	link {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
		icon = R.drawable.ic_tv
	}

	link {
		setTitle(R.string.licenses_link)
		setContent(R.string.licenses_link_description)
		icon = R.drawable.ic_guide
		withFragment<LicensesScreen>()
	}

	action {
		setTitle(R.string.jellyseerr_update_check_title)
		setContent(R.string.jellyseerr_update_check_summary)
		icon = R.drawable.ic_settings
		onActivate = {
			CoroutineScope(Dispatchers.Main).launch {
				UpdateChecker.downloadAndInstall(context, BuildConfig.VERSION_NAME, notifyIfCurrent = true)
			}
		}
	}
}
