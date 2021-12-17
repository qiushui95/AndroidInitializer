import com.vanniktech.maven.publish.AndroidLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    afterEvaluate {
        configure(AndroidLibrary(JavadocJar.Javadoc(), true))
    }
}

task("updateVersion") {
    doFirst {
        val properties = Properties()

        val file = file("gradle.properties")

        properties.load(file.inputStream())

        val oldVersionName = properties["VERSION_NAME"]?.toString() ?: "1.0.0"

        val newVersion = oldVersionName.split(".")
            .map { it.toInt() }
            .run {
                "${get(0)}.${get(1)}.${get(2) + 1}"
            }

        properties["VERSION_NAME"] = newVersion

        properties.store(file.writer(), null)
    }
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 16
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0-RC2")
    compileOnly("androidx.startup:startup-runtime:1.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}