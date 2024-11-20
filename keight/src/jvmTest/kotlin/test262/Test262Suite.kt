package test262

import kotlinx.coroutines.delay
import runtimeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val UNSUPPORTED_FEATURES = listOf(
    "cross-realm", "Proxy", "hashbang", "u180e","generators"
)

class Test262Suite {

    @Test
    fun test() = runtimeTest { r ->
        var i = 0

        var failed = 0
        var ignored = 0
        resources().resolve("language/statements").walk().forEach {
            if (it.isFile) {
                try {
                    val test = Test262Case.fromSource(it)
                    if (!test.features.any { it in UNSUPPORTED_FEATURES }) {
                        r.reset()
                        test.harnessFiles.forEach {
                            evalFile("/harness/$it", r)
                        }
                        test.test(r)
                        println("✅ #${(i++).toString().padEnd(8, ' ')} PASSED     ${it.name}")
                    } else {
                        ignored++
                        println(
                            "⚠\uFE0F #${
                                (i++).toString().padEnd(8, ' ')
                            } IGNORED     ${it.name}"
                        )
                    }
                } catch (t: Throwable) {
                    println("❌ #${(i++).toString().padEnd(8, ' ')} FAILED     ${it.name}",)
                    failed++
//                    println(it.path)
//                    throw t
                }
                if (i % 1000 == 0) {
                    System.gc()
                    delay(5000)
                }
            }
        }
        assertTrue(failed == 0,"Passed ${i-failed} of total $i tests; $ignored ignored; $failed failed")
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}