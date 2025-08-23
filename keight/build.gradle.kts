

kotlin {
    sourceSets {
        wasmJs {
            browser {
                testTask {
                    useKarma {
//                        useChrome()
                        useSafari()
                    }
                }
            }
        }

        js {
            browser {
                testTask {
                    useKarma {
//                        useChrome()
                        useSafari()
                    }
                }
            }
        }

        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.datetime)
            api(project(":keight-core"))
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
