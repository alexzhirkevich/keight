

kotlin {
    sourceSets {

        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        jvmTest.dependencies {
            implementation("org.yaml:snakeyaml:2.3")
        }
    }
}
