package test262

import runtimeTest
import kotlin.test.Test

private val UNSUPPORTED_FEATURES = listOf(
    "cross-realm", "Proxy", "hashbang", "u180e"
)

class Test262Suite {

    @Test
    fun test() = runtimeTest { r ->
        var i = 0

        resources().walk().forEach {
            if (it.isFile) {
                val test = Test262Case.fromSource(it)
                if (!test.features.any { it in UNSUPPORTED_FEATURES}) {
                    r.reset()
                    test.harnessFiles.forEach {
                        evalFile("/harness/$it", r)
                    }
                    test.test(r)
                    println("#${i++}\t ✅ PASSED ${it.name}")
                } else {
                    println("#${i++}\t ⚠\uFE0F IGNORED ${it.name}")
                }
            }
        }
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}