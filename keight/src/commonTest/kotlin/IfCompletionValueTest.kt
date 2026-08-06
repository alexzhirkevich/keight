import kotlin.test.Ignore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Completion-value semantics for `if` / `else` statements.
 *
 * In ECMAScript the completion value of a Script/Program is the completion value of its
 * last StatementListItem. For an `if` statement whose taken branch is an
 * ExpressionStatement, that value is the expression's value — e.g. `eval("if (true) '1'")`
 * yields `'1'` in real JS.
 *
 * keight currently returns `Undefined` when an `if`/`else` chain is the final statement
 * of a program, because `OpIfCondition` is built with `expressible = false` in statement
 * position (see `Parser.kt` `Keyword.If` branch). This is inconsistent with keight's own
 * behavior for expression-last programs (e.g. `eval("'1'") === '1'`) and with the JS spec.
 *
 * The tests below capture the spec-compliant expected behavior and are `@Ignore`d until
 * completion-value propagation for statements is implemented. See the corresponding
 * GitHub issue (completion value for if/else).
 */
class IfCompletionValueTest {

    @Ignore//("if/else completion value not propagated (returns Undefined) — tracked separately from parser fix #22")
    @Test
    fun if_true_completion_value() = runTest {
        """if (true) '1'; else '2';""".trimIndent().eval().assertEqualsTo("1")
    }

    @Ignore//("if/else completion value not propagated (returns Undefined) — tracked separately from parser fix #22")
    @Test
    fun if_false_else_completion_value() = runTest {
        """if (false) '1'; else '2';""".trimIndent().eval().assertEqualsTo("2")
    }

    @Ignore//("if/else completion value not propagated (returns Undefined) — tracked separately from parser fix #22")
    @Test
    fun else_if_chain_completion_value() = runTest {
        """if (false) '1'; else if (true) '2'; else '3';""".trimIndent().eval().assertEqualsTo("2")
    }

    @Ignore//("if/else completion value not propagated (returns Undefined) — tracked separately from parser fix #22")
    @Test
    fun issue_reproducer_completion_value() = runTest {
        """var label = 'a';if (true) '1';else if (true) '2';else '3';""".trimIndent().eval().assertEqualsTo("1")
    }

    @Ignore//("if/else completion value not propagated (returns Undefined) — tracked separately from parser fix #22")
    @Test
    fun block_consequent_completion_value() = runTest {
        """if (true) { '1' }""".trimIndent().eval().assertEqualsTo("1")
    }

    // Control: expression-last programs already return their value. This sanity-checks
    // that the completion-value mechanism works for expressions but NOT for `if`.
    @Test
    fun expression_last_returns_value() = runTest {
        """'1'""".trimIndent().eval().assertEqualsTo("1")
        """var a = 1; a""".trimIndent().eval().assertEqualsTo(1L)
    }
}
