plugins {
    id("root.publication")
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.serialization).apply(false)
}

buildscript {
    dependencies {
        classpath(libs.gp.atomicfu)
        classpath(libs.nexus.publish)
    }
}

subprojects {

    if (!name.startsWith("keight")) {
        return@subprojects
    }

    plugins.apply("module.publication")
    plugins.apply("module.android")
    plugins.apply("module.multiplatform")
}

