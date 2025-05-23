package test262

import assertEqualsTo
import eval
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.delay
import runtimeTest
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

private val UNSUPPORTED_FLAGS = listOf(
    "onlyStrict"
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

    "asi-restriction-invalid.js",
    "asi-restriction-invalid-parenless-parameters-expression-body.js",
    "asi-restriction-invalid-parenless-parameters.js",
    "use-strict-with-non-simple-param.js"
)

class Test262Suite {

    var i = 0

    var failed = 0
    var ignored = 0

    @Test
    fun temp() = runtimeTest {

//        val callable : Callable = "(a,b) => a + b".eval(it) as Callable
        val func = "(function(obj) { return obj.name })".eval(it) as suspend (List<JsAny>) -> Any?

        func(listOf(Object { "name".js() eq "peter".js() })).assertEqualsTo("peter")
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
                    var ignore = false
                    try {
                        val test = Test262Case.fromSource(it)

                        ignore = test.features.any { it in UNSUPPORTED_FEATURES }
                                || UNSUPPORTED_FLAGS.any { test.hasFlag(it) }
                                || it.name in MUTED_TESTS

                        r.reset()
                        test.harnessFiles.forEach {
                            harness(it, r)
                        }
                        test.test(r)
//                        println("✅ #${i.toString().padEnd(8, ' ')} PASSED     ${it.name}")
                    } catch (t: Throwable) {
                        if (ignore) {
                            ignored++
//                            println("⚠\uFE0F #${i.toString().padEnd(8, ' ')} IGNORED     ${it.name}")
                        } else {
                            failed++
                            println("❌ #${i.toString().padEnd(8, ' ')} FAILED     ${it.name}",)
                            throw t
                        }
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