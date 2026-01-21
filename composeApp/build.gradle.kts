import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Kotlin Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

            // Kotlin Serialization for JSON
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            // HTTP Client for Grok API calls
            implementation("io.ktor:ktor-client-core:2.3.9")
            implementation("io.ktor:ktor-client-cio:2.3.9")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")
            implementation("io.ktor:ktor-client-logging:2.3.9")


            // Apache PDFBox for PDF to Image conversion
            implementation("org.apache.pdfbox:pdfbox:3.0.1")
            // Apache POI for Excel (alternative/backup)
            implementation("org.apache.poi:poi:5.2.5")
            implementation("org.apache.poi:poi-ooxml:5.2.5")

            // Logging
            implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
            implementation("ch.qos.logback:logback-classic:1.4.14")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // window-size-class removed to avoid mixing UI versions
        }
    }
}


compose.desktop {
    application {
        mainClass = "ma.zer0ne.ocr.MainKt"


        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Zer0ne-OCR"
            packageVersion = "1.0.0"
            description = "AI-powered Invoice OCR Application"
            vendor = "Zer0ne"
            copyright = "© 2026 Zer0ne. All rights reserved."

            // Include only necessary JDK modules for smaller size
            modules("java.base", "java.desktop", "java.logging", "java.naming", "java.sql", "jdk.unsupported")

            windows {
                menuGroup = "Zer0ne"
                upgradeUuid = "e8c9a5f2-4b3d-4e6a-9f1c-8d7b6a5e4c3d"
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
            }

            linux {
                menuGroup = "Zer0ne"
                shortcut = true
            }

            macOS {
                bundleID = "ma.zer0ne.ocr"
            }
        }
    }
}

// Add a safe JavaExec run task to avoid jvmRun/main-class issues and run the app directly.
// Use `./gradlew :composeApp:runDesktop` to run.
tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Run the Compose desktop application (MainKt) using the jvm runtime classpath"

    // Ensure compiled classes and resources are on the classpath
    val classesDir = layout.buildDirectory.dir("classes/kotlin/jvm/main")
    val resourcesDir = layout.buildDirectory.dir("resources/jvm/main")

    classpath = files(classesDir, resourcesDir) + configurations["jvmRuntimeClasspath"]
    mainClass.set("ma.zer0ne.ocr.MainKt")

    // Rebuild classes before running
    dependsOn(tasks.named("compileKotlinJvm"))
}
