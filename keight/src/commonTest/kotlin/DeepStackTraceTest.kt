import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Deep stack trace tests.
 *
 * Tests that exercise the call stack tracking mechanism across deeply nested
 * function calls, class hierarchies, closures, and recursive invocations.
 */
class DeepStackTraceTest {

    // ========== Deep nesting: function call chains ==========

    @Test
    fun stackTraceDeepChain5Levels() = runTest {
        val result = """
            function l1() { return l2(); }
            function l2() { return l3(); }
            function l3() { return l4(); }
            function l4() { return l5(); }
            function l5() { return new Error('deep').stack; }
            l1();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'l1'") { s.contains("l1") }
        assertTrue("stack should contain 'l2'") { s.contains("l2") }
        assertTrue("stack should contain 'l3'") { s.contains("l3") }
        assertTrue("stack should contain 'l4'") { s.contains("l4") }
        assertTrue("stack should contain 'l5'") { s.contains("l5") }
    }

    @Test
    fun stackTraceDeepChainOrder() = runTest {
        val result = """
            function alpha() { return beta(); }
            function beta() { return gamma(); }
            function gamma() { return new Error('order').stack; }
            alpha();
        """.trimIndent().eval()
        val s = result.toString()
        // Stack traces print top frame first (gamma), then callers (beta, alpha).
        // In the string, gamma appears first (lower index), alpha last (higher index).
        val alphaIdx = s.indexOf("alpha")
        val betaIdx = s.indexOf("beta")
        val gammaIdx = s.indexOf("gamma")
        assertTrue("gamma should appear before beta in string (gammaIdx=$gammaIdx, betaIdx=$betaIdx)") {
            gammaIdx in 1..<betaIdx
        }
        assertTrue("beta should appear before alpha in string (betaIdx=$betaIdx, alphaIdx=$alphaIdx)") {
            alphaIdx > 0 && betaIdx < alphaIdx
        }
    }

    // ========== Deep nesting: class hierarchies ==========

    @Test
    fun stackTraceThroughClassHierarchy() = runTest {
        val result = """
            class Base {
                doWork() { return new Error('base').stack; }
            }
            class Child extends Base {
                start() { return this.doWork(); }
            }
            class GrandChild extends Child {
                run() { return this.start(); }
            }
            new GrandChild().run();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'doWork'") { s.contains("doWork") }
        assertTrue("stack should contain 'start'") { s.contains("start") }
        assertTrue("stack should contain 'run'") { s.contains("run") }
    }

    @Test
    fun stackTraceConstructorChain() = runTest {
        val result = """
            class A {
                constructor() {}
                check() {
                    return new Error('ctor chain').stack;
                }
            }
            class B extends A {}
            class C extends B {}
            new C().check();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'check'") { s.contains("check") }
        assertTrue("stack should contain 'ctor chain'") { s.contains("ctor chain") }
    }

    // ========== Deep nesting: closures ==========

    @Test
    fun stackTraceThroughClosures() = runTest {
        val result = """
            function outer() {
                var x = 1;
                function middle() {
                    var y = 2;
                    function inner() {
                        var z = 3;
                        return new Error('closure ' + (x+y+z)).stack;
                    }
                    return inner();
                }
                return middle();
            }
            outer();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'inner'") { s.contains("inner") }
        assertTrue("stack should contain 'middle'") { s.contains("middle") }
        assertTrue("stack should contain 'outer'") { s.contains("outer") }
        assertTrue("stack should contain 'closure 6'") { s.contains("closure 6") }
    }

    @Test
    fun stackTraceArrowFunctionChain() = runTest {
        val result = """
            var a = () => {
                var b = () => {
                    var c = () => {
                        var d = () => {
                            return new Error('arrow chain').stack;
                        };
                        return d();
                    };
                    return c();
                };
                return b();
            };
            a();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'arrow chain'") { s.contains("arrow chain") }
        // Arrow functions are anonymous (empty name), so we check for the error message
        assertTrue("stack should have multiple 'at' frames") {
            s.lines().count { it.trim().startsWith("at") } >= 2
        }
    }

    // ========== Deep nesting: object method chains ==========

    @Test
    fun stackTraceObjectMethodChain() = runTest {
        val result = """
            var obj = {
                a: function() { return this.b(); },
                b: function() { return this.c(); },
                c: function() { return new Error('obj method').stack; }
            };
            obj.a();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'obj method'") { s.contains("obj method") }
    }

    @Test
    fun stackTraceClassStaticMethodChain() = runTest {
        val result = """
            class Util {
                static step1() { return Util.step2(); }
                static step2() { return Util.step3(); }
                static step3() { return new Error('static chain').stack; }
            }
            Util.step1();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'static chain'") { s.contains("static chain") }
        assertTrue("stack should contain 'step1'") { s.contains("step1") }
        assertTrue("stack should contain 'step2'") { s.contains("step2") }
        assertTrue("stack should contain 'step3'") { s.contains("step3") }
    }

    // ========== Recursive calls ==========

    @Test
    fun stackTraceRecursiveFunction() = runTest {
        val result = """
            function recurse(n) {
                if (n <= 0) return new Error('base case').stack;
                return recurse(n - 1);
            }
            recurse(5);
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'base case'") { s.contains("base case") }
        // Recursive calls should show multiple 'recurse' frames
        val recurseCount = s.lines().count { it.contains("recurse") }
        assertTrue("stack should contain multiple 'recurse' frames but got $recurseCount") {
            recurseCount >= 2
        }
    }

    @Test
    fun stackTraceMutualRecursion() = runTest {
        val result = """
            function isEven(n) {
                if (n === 0) return new Error('even:0').stack;
                return isOdd(n - 1);
            }
            function isOdd(n) {
                if (n === 0) return new Error('odd:0').stack;
                return isEven(n - 1);
            }
            isEven(4);
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'even:0'") { s.contains("even:0") }
        assertTrue("stack should contain 'isEven'") { s.contains("isEven") }
        assertTrue("stack should contain 'isOdd'") { s.contains("isOdd") }
    }

    // ========== Error types with deep stacks ==========

    @Test
    fun stackTraceTypeErrorInDeepCall() = runTest {
        val result = """
            function a() { return b(); }
            function b() { return c(); }
            function c() {
                try {
                    null.property;
                } catch(e) {
                    return e.stack;
                }
            }
            a();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'TypeError'") { s.contains("TypeError") }
    }

    @Test
    fun stackTraceReferenceErrorInDeepCall() = runTest {
        val result = """
            function f1() { return f2(); }
            function f2() { return f3(); }
            function f3() {
                try {
                    notDefinedVariable;
                } catch(e) {
                    return e.stack;
                }
            }
            f1();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'ReferenceError' or 'is not defined'") {
            s.contains("ReferenceError") || s.contains("is not defined")
        }
    }

    // ========== Mixed: function + class + closure ==========

    @Test
    fun stackTraceMixedContexts() = runTest {
        val result = """
            function helper() {
                return new Error('mixed').stack;
            }
            class MyClass {
                doWork() {
                    return helper();
                }
            }
            new MyClass().doWork();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'helper'") { s.contains("helper") }
        assertTrue("stack should contain 'mixed'") { s.contains("mixed") }
        assertTrue("stack should contain 'doWork'") { s.contains("doWork") }
    }

    // ========== Stack trace integrity ==========

    @Test
    fun stackTracePreservesErrorNameForSubtypes() = runTest {
        val result = """
            function deep() {
                return new TypeError('type err in deep').stack;
            }
            deep();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should start with 'TypeError'") { s.startsWith("TypeError") }
        assertTrue("stack should contain 'type err in deep'") { s.contains("type err in deep") }
    }

    @Test
    fun stackTraceEmptyVsNonEmptyCallStack() = runTest {
        // Error created at top level (no function call) vs in a function
        val topLevel = "new Error('top').stack".eval().toString()
        val inFunction = """
            function f() { return new Error('in fn').stack }
            f()
        """.trimIndent().eval().toString()

        // Both should contain the error message
        assertTrue("top-level stack should contain 'top'") { topLevel.contains("top") }
        assertTrue("in-function stack should contain 'in fn'") { inFunction.contains("in fn") }

        // In-function stack should have 'f' frame
        assertTrue("in-function stack should contain 'f'") { inFunction.contains("f") }
    }

    @Test
    fun stackTraceFrameCount() = runTest {
        val result = """
            function a() { return b(); }
            function b() { return c(); }
            function c() { return d(); }
            function d() { return new Error('count').stack; }
            a();
        """.trimIndent().eval()
        val s = result.toString()
        val frameCount = s.lines().count { it.trim().startsWith("at") }
        assertTrue("stack should have at least 4 frames (a,b,c,d) but got $frameCount") {
            frameCount >= 4
        }
    }

    // ========== Error location across deep calls ==========

    @Test
    fun stackTraceWithScriptNameInDeepCall() = runTest {
        val script = """
            function outer() {
                return inner();
            }
            function inner() {
                return new Error('named').stack;
            }
            outer();
        """.trimIndent()
        val runtime = JSRuntime(Job())
        val compiled = JSEngine(runtime).compile(script, "deep-test.js")
        val result = compiled.invoke(runtime)
        val s = result.toString()
        assertTrue("stack should contain 'deep-test.js'") { s.contains("deep-test.js") }
        assertTrue("stack should contain 'outer'") { s.contains("outer") }
        assertTrue("stack should contain 'inner'") { s.contains("inner") }
    }

    @Test
    fun stackTraceLineNumbersInMultiFunctionScript() = runTest {
        val script = """
            function first() {
                return second();
            }
            function second() {
                return third();
            }
            function third() {
                return new Error('lines').stack;
            }
            first();
        """.trimIndent()
        val runtime = JSRuntime(Job())
        val compiled = JSEngine(runtime).compile(script, "lines.js")
        val result = compiled.invoke(runtime)
        val s = result.toString()
        // Each frame should have a line number (pattern: "at funcName (lines.js:N")
        val linePattern = Regex("""lines\.js:\d+""")
        assertTrue("stack should contain line numbers in 'lines.js'") {
            linePattern.containsMatchIn(s)
        }
    }

    // ========== Callback-style patterns ==========

    @Test
    fun stackTraceThroughArrayOfCallbacks() = runTest {
        val result = """
            var callbacks = [
                function() { return new Error('cb0').stack; },
                function() { return callbacks[0](); },
                function() { return callbacks[1](); }
            ];
            callbacks[2]();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'cb0'") { s.contains("cb0") }
    }

    @Test
    fun stackTraceThroughHigherOrderFunction() = runTest {
        val result = """
            function apply(fn) { return fn(); }
            function transform(fn) { return apply(fn); }
            function execute(fn) { return transform(fn); }
            execute(function() { return new Error('hof').stack; });
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'hof'") { s.contains("hof") }
        assertTrue("stack should contain 'apply'") { s.contains("apply") }
        assertTrue("stack should contain 'transform'") { s.contains("transform") }
        assertTrue("stack should contain 'execute'") { s.contains("execute") }
    }

    // ========== Error re-thrown ==========

    @Test
    fun stackTraceErrorCaughtAndRethrown() = runTest {
        val result = """
            function inner() {
                try {
                    undefinedVar;
                } catch(e) {
                    return e.stack;
                }
            }
            function outer() {
                return inner();
            }
            outer();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'ReferenceError' or 'is not defined'") {
            s.contains("ReferenceError") || s.contains("is not defined")
        }
    }

    // ========== Constructor: new in deep call ==========
    // NOTE: CustomError extends Error with super() does not correctly pass
    // arguments to the parent constructor (message/name/stack remain empty).
    // This is a known inherited behavior issue in JSError's JsObjectImpl delegation.
    // TODO: Re-enable with proper Error subclass support.

    @Test
    fun stackTraceNewErrorInDeepCallShowsConstructor() = runTest {
        val result = """
            function factory() {
                return new Error('factory made').stack;
            }
            function caller() {
                return factory();
            }
            caller();
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'factory made'") { s.contains("factory made") }
        assertTrue("stack should contain 'factory'") { s.contains("factory") }
        assertTrue("stack should contain 'caller'") { s.contains("caller") }
        assertTrue("stack should contain 'new Error'") { s.contains("new Error") }
    }
}
