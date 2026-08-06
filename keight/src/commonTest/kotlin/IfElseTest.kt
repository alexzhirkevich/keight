import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Tests for `if ... else` parsing, including the regression from issue #22
 * (`if (cond) stmt; else ...` with explicit semicolons).
 */
class IfElseTest {

    // Issue #22: the exact reproducer must not throw "Unexpected token 'else'".
    @Test
    fun issue_22_inline_semicolon_else() = runTest {
        // first branch taken
        """
        var label = 'a';
        if (true) label='1';
        else if (true) label='2';
        else label='3';
        label
        """.trimIndent().eval().assertEqualsTo("1")

        // first false -> second true
        """
        var label = 'a';
        if (false) label='1';
        else if (true) label='2';
        else label='3';
        label
        """.trimIndent().eval().assertEqualsTo("2")

        // all false -> final else
        """
        var label = 'a';
        if (false) label='1';
        else if (false) label='2';
        else label='3';
        label
        """.trimIndent().eval().assertEqualsTo("3")
    }

    // The standard `;`-less form (valid JS) must keep working after the fix.
    @Test
    fun inline_no_semicolon_else() = runTest {
        """
        var label = 'a';
        if (true) label='1'
        else if (true) label='2'
        else label='3';
        label
        """.trimIndent().eval().assertEqualsTo("1")

        """
        var label = 'a';
        if (false) label='1'
        else if (true) label='2'
        else label='3';
        label
        """.trimIndent().eval().assertEqualsTo("2")
    }

    // Block consequent followed by an optional `;` then `else`.
    @Test
    fun block_consequent_with_semicolon_else() = runTest {
        """
        var label = 'a';
        if (true) { label='1'; };
        else if (true) { label='2'; };
        else { label='3'; };
        label
        """.trimIndent().eval().assertEqualsTo("1")

        """
        var label = 'a';
        if (false) { label='1'; };
        else if (false) { label='2'; };
        else { label='3'; };
        label
        """.trimIndent().eval().assertEqualsTo("3")
    }

    // An explicit `;` between if-consequent and a following statement must NOT be
    // swallowed (no dangling else present). Regression guard for the fix.
    @Test
    fun inline_no_else_does_not_swallow_semicolon() = runTest {
        // `r=2` must still run unconditionally
        "var r=0; if (true) r=1; r=2; r".eval().assertEqualsTo(2L)
        "var r=0; if (false) r=1; r=2; r".eval().assertEqualsTo(2L)
    }

    // Single `if` (no else) with explicit `;` keeps the statement separator intact.
    @Test
    fun single_if_with_semicolon() = runTest {
        "var label='a'; if (true) label='1'; label".eval().assertEqualsTo("1")
        "var label='a'; if (false) label='1'; label".eval().assertEqualsTo("a")
    }

    // Nested `if` inside the else branch, all with trailing semicolons (issue #22 shape).
    @Test
    fun nested_else_if_with_semicolons() = runTest {
        """
        var out = 'x';
        if (false) out='1';
        else if (false) out='2';
        else if (false) out='3';
        else if (true) out='4';
        else out='5';
        out
        """.trimIndent().eval().assertEqualsTo("4")
    }

    // A real `else` still binds to the nearest `if` (no block separator confusion).
    @Test
    fun else_binds_to_nearest_if() = runTest {
        """
        var x = 0;
        if (true) if (false) x = 1; else x = 2;
        x
        """.trimIndent().eval().assertEqualsTo(2L)
    }
}
