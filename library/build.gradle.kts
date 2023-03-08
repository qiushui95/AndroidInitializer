import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.vanniktech.maven.publish")
}

//mavenPublishing {
//    afterEvaluate {
//        configure(AndroidLibrary(JavadocJar.Javadoc(), true))
//    }
//}

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
    compileSdk = 33

    defaultConfig {
        minSdk = 16

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
    namespace = "son.ysy.initializer.android"
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}