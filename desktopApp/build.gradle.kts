import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.koin.core)
    implementation(libs.compose.components.resources)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.mrcoder20.portx.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PortX"
            packageVersion = "5.2.0"
            description = "Ultra-Fast Network Port Scanner"
            vendor = "mr-coder20"
            copyright = "© 2026 mr-coder20"

            modules("java.instrument", "java.management", "java.sql", "jdk.unsupported", "java.naming", "java.desktop", "jdk.security.auth")

            windows {
                iconFile.set(project.file("src/main/resources/ic1.ico"))
                shortcut = true
                menu = true
                menuGroup = "PortX"
                dirChooser = true
                // Ensure the runtime has more memory and clear error reporting
                jvmArgs("-Xmx2G", "-Dcompose.application.configure.stdio=true")
            }
            macOS {
                iconFile.set(project.file("src/main/resources/ic1.icns"))
                bundleID = "com.mrcoder20.portx"
                dockName = "PortX"
                appCategory = "public.app-category.developer-tools"
            }
            linux {
                iconFile.set(project.file("../shared/src/commonMain/composeResources/drawable/ic1.png"))
                shortcut = true
                menuGroup = "Utility"
            }
        }
    }
}