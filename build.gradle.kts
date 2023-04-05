// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.25.1")
    }
}

task("clean", Delete::class) {
    delete = setOf(rootProject.buildDir)
}