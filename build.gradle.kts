plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    group = "de.tiamop23.minecraft"
    version = "0.1.0-SNAPSHOT"
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktfmt(rootProject.libs.versions.ktfmt.get()).kotlinlangStyle()
        }
    }
}
