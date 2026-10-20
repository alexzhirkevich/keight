@file:OptIn(ExperimentalWasmDsl::class)

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library)
    alias(libs.plugins.mavenPublish)
}


rootProject.projectDir.resolve("local.properties").let {
    if (it.exists()) {
        Properties().apply {
            load(FileInputStream(it))
        }.forEach { (k,v)-> rootProject.ext.set(k.toString(), v) }
        System.getenv().forEach { (k,v) ->
            rootProject.ext.set(k, v)
        }
    }
}

kotlin {
    android {
        namespace = group.toString() + path
            .replace("-", "")
            .split(":")
            .joinToString(".")
        compileSdk {
            version = release((findProperty("android.compileSdk") as String).toInt())
        }
    }

}

val _jvmTarget = findProperty("jvmTarget").toString()

subprojects {
    group = findProperty("group") as String
    version = findProperty("version") as String

    if (!name.startsWith("keight")) {
        return@subprojects
    }

    plugins.apply("com.vanniktech.maven.publish")
    plugins.apply("org.jetbrains.kotlin.multiplatform")
    plugins.apply("com.android.kotlin.multiplatform.library")

    project.kotlin {
        explicitApi()

        applyDefaultHierarchyTemplate()

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
            }
        }

        android {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
            }

            namespace = group.toString() + path
                .replace("-", "")
                .split(":")
                .joinToString(".")

            compileSdk {
                version = release((findProperty("android.compileSdk") as String).toInt())
            }

            minSdk {
                version = release((findProperty("android.minSdk") as String).toInt())
            }
        }


        js { // why not ¯\_(ツ)_/¯
            nodejs()
            browser()
        }

        wasmJs {
            nodejs()
            browser()
        }
        wasmWasi {
            nodejs()
        }

        iosArm64()
        iosX64()
        iosSimulatorArm64()
        macosArm64()
        watchosSimulatorArm64()
        watchosArm32()
        watchosArm64()
        watchosDeviceArm64()
        tvosSimulatorArm64()
        tvosArm64()

        linuxX64()
        linuxArm64()
        mingwX64()

        androidNativeArm32()
        androidNativeArm64()
        androidNativeX86()
        androidNativeX64()
    }

    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()

        coordinates(group.toString(), name, version.toString())
        pom {
            name.set("Keight")
            description.set("JavaScript runtime for Kotlin Multiplatform")
            url.set("https://github.com/alexzhirkevich/keight")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("alexzhirkevich")
                    name.set("Alexander Zhirkevich")
                    email.set("sasha.zhirkevich@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/alexzhirkevich/keight")
                connection.set("scm:git:git://github.com/alexzhirkevich/keight.git")
                developerConnection.set("scm:git:git://github.com/alexzhirkevich/keight.git")
            }
        }
    }

}