import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression tests for issue #20:
 * "Crash occurs when there is no return statement in the function."
 *
 * An empty `return` (no value) that is not the last statement used to consume the
 * trailing newline. The enclosing block then saw the following statement (which may
 * start with a keyword such as `if`, `for`, `class`, ...) without a separator and
 * failed with `SyntaxError: Unexpected token 'Keyword'`.
 */
class ReturnTest {

    @Test
    fun empty_return_followed_by_keyword_no_crash() = runTest {
        // `var` after an empty return (branch not taken, so following code runs)
        """
            (function() {
                if (false) return
                var r = 5;
                return r;
            })()
        """.trimIndent().eval().assertEqualsTo(5L)

        // `for` after an empty return
        """
            (function() {
                if (false) return
                for (var i = 0; i < 3; i++) {}
                return i;
            })()
        """.trimIndent().eval().assertEqualsTo(3L)

        // `while` after an empty return
        """
            (function() {
                if (false) return
                var c = 0;
                while (c < 3) c++;
                return c;
            })()
        """.trimIndent().eval().assertEqualsTo(3L)

        // `function` after an empty return
        """
            (function() {
                if (false) return
                function f() { return 1 }
                return f();
            })()
        """.trimIndent().eval().assertEqualsTo(1L)

        // `class` after an empty return
        """
            (function() {
                if (false) return
                class A {}
                return typeof A;
            })()
        """.trimIndent().eval().assertEqualsTo("function")

        // another `if` (with a valued return) after an empty return
        """
            (function() {
                if (false) return
                if (true) return 2;
                return 3;
            })()
        """.trimIndent().eval().assertEqualsTo(2L)

        // two empty returns in a row
        """
            (function() {
                if (false) return
                if (false) return
                return 99;
            })()
        """.trimIndent().eval().assertEqualsTo(99L)
    }

    @Test
    fun empty_return_exact_issue_reproducer() = runTest {
        // Exact snippet from the issue, with explicit semicolons.
        assertTrue {
            """
                (function() {
                    if (true) return
                    if (true) return
                })()
            """.trimIndent().eval() is Undefined
        }
        assertTrue {
            """
                (function() {
                    if (true) return;if (true) return
                })()
            """.trimIndent().eval() is Undefined
        }

        // Exact snippet without semicolons (newline-terminated empty returns).
        assertTrue {
            """
                (function() {
                    if (true) return
                    if (true) return
                })()
            """.trimIndent().eval() is Undefined
        }
    }

    @Test
    fun return_with_value_unchanged() = runTest {
        // A valued return still yields its value.
        """
            (function() {
                if (true) return 1;
                if (true) return 2;
            })()
        """.trimIndent().eval().assertEqualsTo(1L)

        // Empty return in a non-taken branch does not affect a later valued return.
        """
            (function() {
                if (false) return 1;
                return 7;
            })()
        """.trimIndent().eval().assertEqualsTo(7L)
    }

    @Test
    fun empty_return_returns_undefined() = runTest {
        assertTrue {
            """
                (function() {
                    if (false) return
                    return;
                })()
            """.trimIndent().eval() is Undefined
        }
    }
}
