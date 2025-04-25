// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.31.0")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:7.0.3")
    }
}