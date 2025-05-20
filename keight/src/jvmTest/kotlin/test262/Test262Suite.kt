package test262

import assertEqualsTo
import eval
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
    "eval-var-scope-syntax-err.js",
    "lexical-super-property-from-within-constructor.js",
    "lexical-super-property.js",
    "param-dflt-yield-id-strict.js",
    "rest-param-strict-body.js",
    "rest-params-trailing-comma-early-error.js",
    "scope-body-lex-distinct.js",
    "scope-param-elem-var-close.js",
    "scope-param-elem-var-open.js",
    "scope-param-rest-elem-var-close.js",
    "scope-param-rest-elem-var-open.js",
)

class Test262Suite {

    var i = 0

    var failed = 0
    var ignored = 0

    @Test
    fun temp() = runtimeTest {
        """
            function F() {
              this.af = _ => {
                return this;
              };
            }

            var usurper = {};
            var f = new F();

            throw f.af.bind(usurper)() == f
//            assert.sameValue(f.af.call(usurper), f);
//            assert.sameValue(f.af.bind(usurper)(), f);
        """.eval(it)
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