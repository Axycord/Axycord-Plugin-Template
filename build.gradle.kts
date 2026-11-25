import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("mmaarij.axycord.plugin")
}

val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.exists()) releaseSigningFile.inputStream().use(::load)
}

axycordPlugin {
    id.set("axycord.sample")
    entrypoint.set("mmaarij.axycord.sampleplugin.SamplePlugin")
    description.set("Axycord plugin examples maintained by Maarij")
    // Plugin API levels, independent of the app's versionName and SDK artifact version.
    axycordVersionMinimum.set(3)
    axycordVersionMaximumExclusive.set(4)
    usesResources.set(true)
    async()
}

android {
    namespace = "mmaarij.axycord.sampleplugin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mmaarij.axycord.sampleplugin"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (releaseSigningFile.exists()) create("release") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures.viewBinding = true
    lint {
        checkOnly += setOf("NewApi")
        fatal += "NewApi"
    }
}

dependencies {
    val checkout = providers.gradleProperty("axycordCheckout").orNull
    if (checkout != null) {
        val sdk = file(checkout).resolve("build/plugin-sdk/axycord-plugin-api-1.0.6.jar")
        require(sdk.isFile) { "Generate the local SDK first: scripts/build.sh pluginSdkJar" }
        compileOnly(files(sdk))
    } else {
        compileOnly("mmaarij.axycord:plugin-api:1.0.6")
    }
    compileOnly(libs.rxjava)
}
