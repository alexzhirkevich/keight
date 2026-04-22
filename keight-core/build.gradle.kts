
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.datetime)
            api(libs.jetbrains.annotations)
        }

        all {
            compilerOptions.freeCompilerArgs.add("-XXLanguage:+ContextParameters")
        }
    }
}
