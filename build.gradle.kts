import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.mavenPublish)
}

buildscript {
    dependencies {
        classpath(libs.nexus.publish)
    }
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
    jvm()
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
    plugins.apply("android-library")

    androidLibrarySetup()

    project.kotlin {
        explicitApi()

        applyDefaultHierarchyTemplate()

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
            }
        }

        androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
            }
            publishLibraryVariants("release")
        }


        js(IR) { // why not ¯\_(ツ)_/¯
            nodejs()
            browser()
        }

        wasmJs() {
            nodejs()
            browser()
        }
        wasmWasi {
            nodejs()
        }

        iosArm64()
        iosX64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
        watchosSimulatorArm64()
        watchosX64()
        watchosArm32()
        watchosArm64()
        watchosDeviceArm64()
        tvosSimulatorArm64()
        tvosX64()
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

fun Project.androidLibrarySetup() {
    extensions.configure<LibraryExtension> {
        namespace = group.toString() + path.replace("-", "").split(":").joinToString(".")
        compileSdk = (findProperty("android.compileSdk") as String).toInt()

        defaultConfig {
            minSdk = (findProperty("android.minSdk") as String).toInt()
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
}
