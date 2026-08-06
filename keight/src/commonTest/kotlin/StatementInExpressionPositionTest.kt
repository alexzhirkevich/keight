import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * A statement keyword must not be accepted in an expression position.
 *
 * keight's parser is statement/expression agnostic, so without an explicit guard
 * `var a = if (true) '1'` or `var a = () => if (true) '1'` silently parsed as an
 * if-statement instead of raising a SyntaxError like a spec-compliant engine does.
 *
 * The guard is installed in every position that requires an expression: assignment /
 * initializer right-hand sides, concise arrow bodies, parenthesized groups (call and `new`
 * arguments), array elements, both ternary branches and the `return` / `throw` operands.
 */
class StatementInExpressionPositionTest {

    // The original reproducer: `if` is a statement and can never be an initializer.
    @Test
    fun if_is_not_allowed_as_initializer() = runTest {
        assertFailsWith<SyntaxError> {
            """
            var label = 'a';
            var a = if (true) '1';
            else if (true) '2';
            else '3';
            """.trimIndent().eval()
        }
    }

    @Test
    fun loops_are_not_allowed_as_initializer() = runTest {
        assertFailsWith<SyntaxError> {
            "var a = for (var i = 0; i < 1; i++) i;".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = while (false) 1;".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = do 1; while (false);".eval()
        }
    }

    @Test
    fun other_statements_are_not_allowed_as_initializer() = runTest {
        assertFailsWith<SyntaxError> {
            "var a = switch (1) { case 1: 'x'; };".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = try { 1 } catch (e) { 2 };".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = throw new Error('x');".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = var b = 1;".eval()
        }
    }

    // Plain assignments (no declaration keyword) go through the same path.
    @Test
    fun statement_is_not_allowed_on_assignment_rhs() = runTest {
        assertFailsWith<SyntaxError> {
            "var a = 0; a = if (true) '1';".eval()
        }
        assertFailsWith<SyntaxError> {
            "var o = {}; o.x = if (true) '1';".eval()
        }
        assertFailsWith<SyntaxError> {
            "var o = {}; o['x'] = if (true) '1';".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = 0; a += if (true) 1;".eval()
        }
    }

    // `function` and `class` DO have expression forms and must keep working.
    @Test
    fun function_and_class_expressions_are_still_allowed() = runTest {
        "var f = function () { return 1 }; f()".eval().assertEqualsTo(1L)
        "var C = class { m() { return 2 } }; new C().m()".eval().assertEqualsTo(2L)
        "var f = () => 3; f()".eval().assertEqualsTo(3L)
    }

    // Regular expressions on the right-hand side must not be affected by the guard.
    @Test
    fun regular_initializers_are_unaffected() = runTest {
        "var a = true ? '1' : '2'; a".eval().assertEqualsTo("1")
        "var a = 1 + 2; a".eval().assertEqualsTo(3L)
        "var a = [1, 2]; a[1]".eval().assertEqualsTo(2L)
        "var a = { x: 1 }; a.x".eval().assertEqualsTo(1L)
        "var a = new Error('x'); a.message".eval().assertEqualsTo("x")
        "var a = typeof 1; a".eval().assertEqualsTo("number")
        "var a = null; a".eval().assertEqualsTo(null)
        "var a = this; typeof a".eval().assertEqualsTo("object")
    }

    // Statements themselves must still parse fine outside of expression positions.
    @Test
    fun statements_still_parse_outside_expression_positions() = runTest {
        """
        var label = 'a';
        if (true) label = '1';
        else label = '2';
        label
        """.trimIndent().eval().assertEqualsTo("1")

        "var r = 0; for (var i = 0; i < 3; i++) r += i; r".eval().assertEqualsTo(3L)
        "var r = 0; while (r < 2) r++; r".eval().assertEqualsTo(2L)
        "var r = 0; switch (1) { case 1: r = 5; break; }; r".eval().assertEqualsTo(5L)
    }

    // A concise arrow body is an AssignmentExpression, not a statement.
    @Test
    fun statement_is_not_allowed_as_concise_arrow_body() = runTest {
        assertFailsWith<SyntaxError> {
            """
            var a = () => if (true) '1';
            else if (true) '2';
            else '3';
            a()
            """.trimIndent().eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = () => for (var i = 0; i < 1; i++) i; a()".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = () => while (false) 1; a()".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = () => switch (1) { case 1: 'x'; }; a()".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = () => throw new Error('x'); a()".eval()
        }
        assertFailsWith<SyntaxError> {
            "var a = x => if (x) 1; a(1)".eval()
        }
    }

    @Test
    fun statement_is_not_allowed_as_call_argument() = runTest {
        assertFailsWith<SyntaxError> {
            "function f(x) { return x }; f(if (true) 1)".eval()
        }
        assertFailsWith<SyntaxError> {
            "new Error(if (true) 'x')".eval()
        }
        assertFailsWith<SyntaxError> {
            "(if (true) 1)".eval()
        }
    }

    @Test
    fun statement_is_not_allowed_as_array_element() = runTest {
        assertFailsWith<SyntaxError> {
            "[if (true) 1]".eval()
        }
        assertFailsWith<SyntaxError> {
            "[1, while (false) 2]".eval()
        }
    }

    @Test
    fun statement_is_not_allowed_in_ternary_branches() = runTest {
        assertFailsWith<SyntaxError> {
            "true ? if (true) 1 : 2".eval()
        }
        assertFailsWith<SyntaxError> {
            "true ? 1 : if (true) 2".eval()
        }
    }

    @Test
    fun statement_is_not_allowed_after_return_or_throw() = runTest {
        assertFailsWith<SyntaxError> {
            "function f() { return if (true) 1 }; f()".eval()
        }
        assertFailsWith<SyntaxError> {
            "function f() { return switch (1) { case 1: 'x'; } }; f()".eval()
        }
        assertFailsWith<SyntaxError> {
            "throw if (true) 1".eval()
        }
    }

    // The extra guards must not reject any legal expression form.
    @Test
    fun legal_expressions_in_guarded_positions_are_unaffected() = runTest {
        // arrow bodies
        "var f = () => { if (true) return 1; return 2 }; f()".eval().assertEqualsTo(1L)
        "var f = x => x + 1; f(1)".eval().assertEqualsTo(2L)
        "var f = (a, b) => a + b; f(1, 2)".eval().assertEqualsTo(3L)
        "var f = () => function () { return 4 }; f()()".eval().assertEqualsTo(4L)
        "var f = (a = 5) => a; f()".eval().assertEqualsTo(5L)

        // parenthesized groups: call args, `new`, grouping, statement heads
        "function f(x) { return x }; f(1 + 1)".eval().assertEqualsTo(2L)
        "new Error('x').message".eval().assertEqualsTo("x")
        "(1 + 2)".eval().assertEqualsTo(3L)
        "typeof (1)".eval().assertEqualsTo("number")
        "var r = 0; if (true) r = 1; r".eval().assertEqualsTo(1L)
        "var r = 0; do { r++ } while (false); r".eval().assertEqualsTo(1L)
        "try { throw new Error('x') } catch (e) { e.message }".eval().assertEqualsTo("x")

        // array elements, ternary branches, return/throw operands
        "[1, 2, 3][2]".eval().assertEqualsTo(3L)
        "[1, , 3].length".eval().assertEqualsTo(3L)
        "true ? 1 : 2".eval().assertEqualsTo(1L)
        "false ? 1 : true ? 2 : 3".eval().assertEqualsTo(2L)
        "function f() { return }; typeof f()".eval().assertEqualsTo("undefined")
        "function f() { return 1 + 1 }; f()".eval().assertEqualsTo(2L)
        "function f() { return function () { return 6 } }; f()()".eval().assertEqualsTo(6L)
        "try { throw 'x' } catch (e) { e }".eval().assertEqualsTo("x")
    }
}
