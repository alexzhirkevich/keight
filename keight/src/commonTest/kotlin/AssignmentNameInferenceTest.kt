import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * V8 stack-name semantics.
 *
 * - Lexical variable binding (`const f = fn`) sets the `name` property:
 *   `f.name === "f"` and the stack shows `at f`.
 * - Property assignment (`obj.foo = fn`, `obj["key"] = fn`) leaves `.name` empty
 *   (V8 only sets `name` for lexical bindings and named expressions) but infers a
 *   *frame* name from the left-hand side: `at obj.foo`, `at obj.key`,
 *   `at obj.<computed> [as X]`. The inferred name is recorded on first assignment
 *   only (a function re-assigned to another property keeps the first name).
 * - Named function/class expressions and already-named functions are untouched.
 */
class AssignmentNameInferenceTest {

    @Test
    fun constAssignmentInfersNameInStack() = runTest {
        val result = """
            const f = function() {
                return new Error('boom').stack;
            };
            const s = f();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at f' but got: $s") { s.contains("at f") }
    }

    @Test
    fun constAssignmentSetsNameProperty() = runTest {
        "const f = function(){}; f.name;".eval().assertEqualsTo("f")
    }

    @Test
    fun varAssignmentSetsNameProperty() = runTest {
        "var f = function(){}; f.name;".eval().assertEqualsTo("f")
    }

    @Test
    fun letAssignmentSetsNameProperty() = runTest {
        "let f = function(){}; f.name;".eval().assertEqualsTo("f")
    }

    @Test
    fun arrowFunctionAssignmentInfersName() = runTest {
        "const f = () => {}; f.name;".eval().assertEqualsTo("f")
    }

    @Test
    fun arrowPropertyAssignmentInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj.foo = () => {
                return new Error('boom').stack;
            };
            const s = obj.foo();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.foo' but got: $s") { s.contains("at obj.foo") }
    }

    @Test
    fun arrowComputedStringKeyInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj["key"] = () => {
                return new Error('boom').stack;
            };
            const s = obj["key"]();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.key' but got: $s") { s.contains("at obj.key") }
    }

    @Test
    fun asyncArrowAssignmentInfersName() = runTest {
        "const g = async () => {}; g.name;".eval().assertEqualsTo("g")
    }

    @Test
    fun asyncArrowPropertyAssignmentInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj.foo = async () => {
                throw new Error('boom');
            };
            const s = await obj.foo().catch(e => e.stack);
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.foo' but got: $s") { s.contains("at obj.foo") }
    }

    @Test
    fun propertyAssignmentLeavesNameEmpty() = runTest {
        // V8 keeps `.name` empty for property assignment.
        "const obj = {}; obj.foo = function(){}; obj.foo.name;".eval().assertEqualsTo("")
    }

    @Test
    fun propertyAssignmentInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj.foo = function() {
                return new Error('boom').stack;
            };
            const s = obj.foo();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.foo' but got: $s") { s.contains("at obj.foo") }
    }

    @Test
    fun nestedPropertyAssignmentInfersFrameName() = runTest {
        val result = """
            const ns = { grp : {} };
            ns.grp.fn = function() {
                return new Error('boom').stack;
            };
            const s = ns.grp.fn();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at ns.grp.fn' but got: $s") { s.contains("at ns.grp.fn") }
    }

    @Test
    fun computedStringKeyInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj["key"] = function() {
                return new Error('boom').stack;
            };
            const s = obj["key"]();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.key' but got: $s") { s.contains("at obj.key") }
    }

    @Test
    fun computedNumericKeyInfersFrameName() = runTest {
        val result = """
            const obj = {};
            obj[7] = function() {
                return new Error('boom').stack;
            };
            const s = obj[7]();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.7' but got: $s") { s.contains("at obj.7") }
    }

    @Test
    fun computedRuntimeKeyInfersComputedFrameName() = runTest {
        val result = """
            const obj = {};
            const k = "dyn";
            obj[k] = function() {
                return new Error('boom').stack;
            };
            const s = obj[k]();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.<computed> [as dyn]' but got: $s") {
            s.contains("at obj.<computed> [as dyn]")
        }
    }

    @Test
    fun firstAssignmentWinsForFrameName() = runTest {
        // A function assigned to two different properties keeps the first name,
        // exactly like V8 (`obj.bar = obj.foo` shows `at obj.foo`).
        val result = """
            const obj = {};
            obj.foo = function() {
                return new Error('boom').stack;
            };
            obj.bar = obj.foo;
            const s = obj.bar();
            s;
        """.trimIndent().eval()
        val s = result.toString()
        assertTrue("stack should contain 'at obj.foo' but got: $s") { s.contains("at obj.foo") }
    }

    @Test
    fun namedFunctionExpressionIsNotOverridden() = runTest {
        "const f = function g(){}; f.name;".eval().assertEqualsTo("g")
    }

    @Test
    fun assignmentDoesNotRenameAlreadyNamedFunction() = runTest {
        """
            function real() { return 0; }
            const f = real;
            f.name;
        """.trimIndent().eval().assertEqualsTo("real")
    }

    @Test
    fun inlineAnonymousFunctionShowsLocationNotAnonymous() = runTest {
        val result = """
            (function() {
                return new Error('boom').stack;
            })();
        """.trimIndent().eval()
        val s = result.toString()
        // V8 drops <anonymous> for any frame that has a source location; it renders the
        // location directly instead. The inline IIFE still has a real line/column.
        assertTrue("inline anonymous fn should NOT carry <anonymous> but got: $s") { !s.contains("<anonymous>") }
        assertTrue("inline anonymous fn should still show a source location but got: $s") {
            s.contains("at :")
        }
    }

    @Test
    fun classExpressionAssignmentInfersName() = runTest {
        "const C = class {}; C.name;".eval().assertEqualsTo("C")
    }
}
