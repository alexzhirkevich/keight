import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Completion-value semantics for `if` / `else` statements (Issue #23).
 *
 * In ECMAScript the completion value of a Script/Program is the completion value of its
 * last StatementListItem. For an `if` statement whose taken branch is an
 * ExpressionStatement, that value is the expression's value — e.g. `eval("if (true) '1'")`
 * yields `'1'` in real JS.
 *
 * These tests were previously `@Ignore`d because keight returned `Undefined` for an `if`/`else`
 * chain used as a program's final statement. The fix makes `OpIfCondition` propagate the taken
 * branch's value (the statement form now uses `expressible = true`), matching the spec.
 */
class IfCompletionValueTest {

    @Test
    fun if_true_completion_value() = runTest {
        """if (true) '1'; else '2';""".trimIndent().eval().assertEqualsTo("1")
    }

    @Test
    fun if_false_else_completion_value() = runTest {
        """if (false) '1'; else '2';""".trimIndent().eval().assertEqualsTo("2")
    }

    @Test
    fun else_if_chain_completion_value() = runTest {
        """if (false) '1'; else if (true) '2'; else '3';""".trimIndent().eval().assertEqualsTo("2")
    }

    @Test
    fun issue_reproducer_completion_value() = runTest {
        """var label = 'a';if (true) '1';else if (true) '2';else '3';""".trimIndent().eval().assertEqualsTo("1")
    }

    @Test
    fun block_consequent_completion_value() = runTest {
        """if (true) { '1' }""".trimIndent().eval().assertEqualsTo("1")
    }

    @Test
    fun if_false_without_else_is_undefined() = runTest {
        // `if (false) '1'` (no else) still completes with `undefined` — the taken branch
        // is absent, so there is no value to propagate.
        """if (false) '1'""".trimIndent().eval().assertEqualsTo(Undefined)
    }

    // Control: expression-last programs already return their value. This sanity-checks
    // that the completion-value mechanism works for expressions.
    @Test
    fun expression_last_returns_value() = runTest {
        """'1'""".trimIndent().eval().assertEqualsTo("1")
        """var a = 1; a""".trimIndent().eval().assertEqualsTo(1L)
    }
}
