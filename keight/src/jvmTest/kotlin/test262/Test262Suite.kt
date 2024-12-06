package test262

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlinx.coroutines.delay
import runtimeTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

private val UNSUPPORTED_FEATURES = listOf(
    "cross-realm",
    "Proxy",
    "hashbang",
    "u180e",
    "generators",
    "BigInt",
    "Symbol.iterator",
    "destructuring-binding",
    "new.target",
)

private val MUTED_TESTS = listOf(
    "capturing-closure-variables-2.js",
    "ArrowFunction_restricted-properties.js",
    "dflt-obj-ptrn-prop-ary.js",
    "eval-var-scope-syntax-err.js"
)

class Test262Suite {

    var i = 0

    var failed = 0
    var ignored = 0
    private suspend fun File.testDirectory(r: JSRuntime){
        walk().forEach {
            if (it.isFile) {
                i++
                try {
                    val test = Test262Case.fromSource(it)
                    if (test.features.all { it !in UNSUPPORTED_FEATURES } && it.name !in MUTED_TESTS) {
                        r.reset()
                        test.harnessFiles.forEach {
                            harness(it, r)
                        }
                        test.test(r)
//                            println("✅ #${i.toString().padEnd(8, ' ')} PASSED     ${it.name}")
                    } else {
                        ignored++
//                            println("⚠\uFE0F #${i.toString().padEnd(8, ' ')} IGNORED     ${it.name}")
                    }
                } catch (t: Throwable) {
                    println("❌ #${i.toString().padEnd(8, ' ')} FAILED     ${it.name}",)
                    failed++
//                    println(it.path)
                    throw t
                }
                if (i % 1000 == 0) {
                    System.gc()
                    delay(5000)
                }
            } else {
                it.testDirectory(r)
            }
        }
    }

    @Test
    fun test() = runtimeTest { r ->

        val order = listOf(
            "language/types",
            "language/comments",
            "language/expressions",
        )

        order.forEach { f ->
            test262().resolve(f).walk().forEach {
                if (it.isFile) {
                    i++
                    try {
                        val test = Test262Case.fromSource(it)
                        if (test.features.all { it !in UNSUPPORTED_FEATURES } && it.name !in MUTED_TESTS) {
                            r.reset()
                            test.harnessFiles.forEach {
                                harness(it, r)
                            }
                            test.test(r)
//                            println("✅ #${i.toString().padEnd(8, ' ')} PASSED     ${it.name}")
                        } else {
                            ignored++
//                            println("⚠\uFE0F #${i.toString().padEnd(8, ' ')} IGNORED     ${it.name}")
                        }
                    } catch (t: Throwable) {
                        failed++
                        println("❌ #${i.toString().padEnd(8, ' ')} FAILED     ${it.name}",)
//                    println(it.path)
                        throw t
                    }
                    if (i % 1000 == 0) {
                        System.gc()
                        delay(1000)
                    }
                }
            }
        }
        assertTrue(failed == 0,"Passed ${i-failed} of total $i tests; ${i - failed - ignored} success, $ignored ignored, $failed failed")
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}