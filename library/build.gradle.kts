import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.vanniktech.maven.publish")
}

tasks.register("updateVersion") {
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
    compileSdk = 35

    defaultConfig {
        minSdk = 16

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }

    namespace = "son.ysy.initializer.android"
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}