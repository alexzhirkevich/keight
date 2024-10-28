
plugins {
    id("module.android")
    id("module.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}
