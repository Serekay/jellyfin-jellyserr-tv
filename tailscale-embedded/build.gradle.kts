plugins {
	id("com.android.library")
}

android {
	namespace = "com.tailscale.embedded"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		consumerProguardFiles("consumer-rules.pro")
	}

	buildFeatures {
		buildConfig = false
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
}

dependencies {
	// Re-export the prebuilt Tailscale Android core (produced via third_party/tailscale-android).
	api(files("../third_party/tailscale-android/android/libs/libtailscale.aar"))
}
