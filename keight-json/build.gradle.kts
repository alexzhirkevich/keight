
plugins {
    id("module.android")
    id("module.multiplatform")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(project(":keight"))
            implementation(libs.serialization)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
