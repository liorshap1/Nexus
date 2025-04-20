// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    kotlin("kapt") version "2.1.20"
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.diffplug.spotless") version "7.0.3"
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}
subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("**/*.java")
            targetExclude("$layout/**/*.java")

            // Replace this:
            // googleJavaFormat("1.17.0")

            // With this:
            eclipse().configFile(rootProject.file("spotless/eclipse-java-formatter.xml"))

            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
        }

        kotlin {
            target("**/*.kt")
            ktlint()
            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
        }

        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}
