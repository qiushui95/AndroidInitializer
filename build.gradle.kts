// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    }
}

task("clean", Delete::class) {
    delete = setOf(rootProject.buildDir)
}