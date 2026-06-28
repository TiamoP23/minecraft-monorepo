plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "net.clans"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
