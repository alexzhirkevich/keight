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
import kotlin.test.Ignore
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
    "dynamic-import"
)

private val UNSUPPORTED_FLAGS = listOf(
    "onlyStrict",
    "module"
)

private val MUTED_TESTS = listOf(
    "S8.3_A1_T1.js", // invalid
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
    "use-strict-with-non-simple-param.js",

    "order-of-evaluation.js", // todo: fix araylike keys
    "this.js",
    "S11.8.1_A4.4.js", // -0.0, +0.0
    "S11.8.1_A3.2_T1.2.js", // function.toString() - no code
)

class Test262Suite {

    var i = 0

    var failed = 0
    var ignored = 0
    var passed = 0

    @Test
    @Ignore
    fun test() = runtimeTest { r ->

        val order = listOf(
            "language/types",
            "language/comments",
            "language/expressions",
            "language/statements",
            "language/block-scope",
            "language/function-code",
            "language/global-code",
            "language/identifiers",
            "language/keywords",
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
                                || it.name.contains("FIXTURE")

                        r.reset()
                        test.harnessFiles.forEach {
                            harness(it, r)
                        }
                        test.test(r)
                        passed++
//                        println("✅ #${i.toString().padEnd(8, ' ')} PASSED     ${it.name}")
                    } catch (t: Throwable) {
                        if (ignore) {
                            ignored++
//                            println("⚠\uFE0F #${i.toString().padEnd(8, ' ')} IGNORED     ${it.name}")
                        } else {
                            failed++
                            println("❌ #${i.toString().padEnd(8, ' ')} FAILED     ${it.name}; IGNORED: $ignored; PASSED: $passed",)
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
        assertTrue(failed == 0,"Passed $passed of total $i tests; $ignored ignored, $failed failed")
//        resources().resolve("language/types/boolean")
//            .listFiles(FileFilter { it.isFile })!!
//            .forEach {
//                Test262Case.fromSource(it).test(r)
//            }
    }
}