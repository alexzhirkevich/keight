import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

val _jvmTarget = findProperty("jvmTarget")!! as String

kotlin {
    explicitApi()

    applyDefaultHierarchyTemplate()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
        }
    }

//    androidTarget {
//        compilerOptions {
//            jvmTarget.set(JvmTarget.fromTarget(_jvmTarget))
//        }
//        publishLibraryVariants("release")
//    }

//
//    js(IR) { // why not ¯\_(ツ)_/¯
//        nodejs()
//        browser()
//    }
//
//    wasmJs() {
//        nodejs()
//        browser()
//    }
//    wasmWasi {
//        nodejs()
//    }

//    iosArm64()
//    iosX64()
//    iosSimulatorArm64()
//    macosX64()
//    macosArm64()
//    watchosSimulatorArm64()
//    watchosX64()
//    watchosArm32()
//    watchosArm64()
//    watchosDeviceArm64()
//    tvosSimulatorArm64()
//    tvosX64()
//    tvosArm64()
//
//    linuxX64()
//    linuxArm64()
//    mingwX64()
//
//    androidNativeArm32()
//    androidNativeArm64()
//    androidNativeX86()
//    androidNativeX64()
}