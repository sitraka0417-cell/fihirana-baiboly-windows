import java.io.InputStream
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "mg.sitraka.fihirana"
version = "1.1.0"

kotlin { jvmToolchain(17) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    // JavaFX Media: Windows-native playback for projection backgrounds.
    implementation("org.openjfx:javafx-base:17.0.14:win")
    implementation("org.openjfx:javafx-graphics:17.0.14:win")
    implementation("org.openjfx:javafx-controls:17.0.14:win")
    implementation("org.openjfx:javafx-media:17.0.14:win")
    implementation("org.openjfx:javafx-swing:17.0.14:win")
}

compose.desktop {
    application {
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        mainClass = "mg.sitraka.fihirana.windows.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            // FIX "Failed to launch JVM": jpackage/jlink ne garde par défaut que quelques modules JDK
            // (java.base, java.desktop, jdk.unsupported...). sqlite-jdbc a besoin de java.sql, java.logging,
            // java.naming, etc. Sans eux, le .exe installé s'arrête aussitôt avec "Failed to launch JVM".
            modules(
                "java.sql", "java.naming", "java.logging", "java.management", "java.prefs",
                "java.xml", "java.net.http", "jdk.unsupported", "jdk.jsobject", "jdk.xml.dom"
            )
            jvmArgs += listOf("-Xmx1g", "-Dfile.encoding=UTF-8")
            packageName = "BaibolySyFihiranaAdvantista"
            packageVersion = "1.1.0"
            description = "Baiboly sy Fihirana Advantista"
            vendor = "Sitraka Nambinintsoa"
            windows {
                menuGroup = "Baiboly sy Fihirana"
                upgradeUuid = "9b11b36b-2a4a-4e3e-91a2-5f0b7c89d221"
            }
        }
    }
}

val downloadContentDb by tasks.registering {
    val output = layout.projectDirectory.file("src/main/resources/fihirana_baiboly.db")
    outputs.file(output)
    doLast {
        val target = output.asFile
        target.parentFile.mkdirs()
        if (!target.exists() || target.length() < 10_000_000L) {
            val url = URL(
                "https://raw.githubusercontent.com/sitraka0417-cell/fihirana/main/app/src/main/assets/fihirana_baiboly.db"
            )
            url.openStream().use { input: InputStream ->
                target.outputStream().use { outputStream -> input.copyTo(outputStream) }
            }
        }
        require(target.length() > 10_000_000L) {
            "Tsy nety nalaina ny base de données original. Hamarino ny connexion Internet."
        }
    }
}
tasks.named("processResources") { dependsOn(downloadContentDb) }
