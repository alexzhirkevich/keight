
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.datetime)
        }
    }
}
