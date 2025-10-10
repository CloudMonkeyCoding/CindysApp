plugins {
    alias(libs.plugins.android.application)
}

val apiBaseUrl = (project.findProperty("API_BASE_URL") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "https://evotech.slarenasitsolutions.com/"

val shiftSchedulePath = (project.findProperty("SHIFT_SCHEDULE_PATH") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "PHP/shift_functions.php"

val shiftActionPath = (project.findProperty("SHIFT_ACTION_PATH") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: shiftSchedulePath

val shiftFetchAction = (project.findProperty("SHIFT_FETCH_ACTION") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "get_shift_schedules"

val shiftStartAction = (project.findProperty("SHIFT_START_ACTION") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "start_shift"

val userProfilePath = (project.findProperty("USER_PROFILE_PATH") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "PHP/user_api.php"

val userProfileAction = (project.findProperty("USER_PROFILE_ACTION") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "get_profile"

val defaultStaffUserId = (project.findProperty("DEFAULT_STAFF_USER_ID") as? String)
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 0

fun String.toBuildConfigString(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.deliveryapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.deliveryapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.toBuildConfigString()}\"")
        buildConfigField("String", "SHIFT_SCHEDULE_PATH", "\"${shiftSchedulePath.toBuildConfigString()}\"")
        buildConfigField("String", "SHIFT_ACTION_PATH", "\"${shiftActionPath.toBuildConfigString()}\"")
        buildConfigField("String", "SHIFT_FETCH_ACTION", "\"${shiftFetchAction.toBuildConfigString()}\"")
        buildConfigField("String", "SHIFT_START_ACTION", "\"${shiftStartAction.toBuildConfigString()}\"")
        buildConfigField("String", "USER_PROFILE_PATH", "\"${userProfilePath.toBuildConfigString()}\"")
        buildConfigField("String", "USER_PROFILE_ACTION", "\"${userProfileAction.toBuildConfigString()}\"")
        buildConfigField("int", "DEFAULT_STAFF_USER_ID", defaultStaffUserId.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
