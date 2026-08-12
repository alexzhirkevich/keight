import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for inline `;`-terminated statements immediately followed by a continuation
 * keyword — the broader family of issue #22.
 *
 * Root cause (shared with `if/else`): `parseDoWhileLoop` / `parseTryCatch` used
 * `eat(Keyword.While/Catch/Finally)`, and `eat` (via `nextSignificant`) skips
 * newlines but not `;`. An explicit `;` left after an inline body made the `eat`
 * see `;` instead of the continuation keyword, orphaning the keyword so it was
 * later parsed as a standalone statement -> SyntaxError.
 *
 * The fix (`eatKeywordAfterOptionalSemicolon`) only consumes the optional `;` when
 * the target keyword actually follows, leaving the `;` in the stream otherwise.
 */
class TryDoInlineTest {

    // --- do/while ----------------------------------------------------------

    // Issue #22 do/while variant: `do x=1; while (false)` must NOT throw
    // "Missing while condition in do/while block".
    @Test
    fun do_while_inline_with_semicolon() = runTest {
        "var label='a'; do label='1'; while (false); label".eval().assertEqualsTo("1")
    }

    // The standard `;`-less form (valid JS) must keep working after the fix.
    @Test
    fun do_while_inline_no_semicolon() = runTest {
        "var label='a'; do label='1' while (false); label".eval().assertEqualsTo("1")
    }

    // Block body followed by an optional `;` then `while`.
    @Test
    fun do_while_block_with_semicolon() = runTest {
        "var label='a'; do { label='1'; }; while (false); label".eval().assertEqualsTo("1")
    }

    // An explicit `;` between the do-body and a following statement must NOT be
    // swallowed (no dangling while present). Regression guard for the fix.
    @Test
    fun do_while_no_semicolon_does_not_swallow() = runTest {
        "var r=0; do r=1; while (false); r=2; r".eval().assertEqualsTo(2L)
        "var r=0; do r=1 while (false); r=2; r".eval().assertEqualsTo(2L)
    }

    // Loop actually iterates with a real condition and trailing `;`.
    @Test
    fun do_while_counts_with_semicolon() = runTest {
        "var c=0; do c=c+1; while (c<3); c".eval().assertEqualsTo(3L)
    }

    // --- try/catch/finally -------------------------------------------------

    // Issue #22 try/catch variant: `try x=1; catch (e) x=2` must NOT throw
    // "Missing catch or finally after try".
    @Test
    fun try_catch_inline_with_semicolon() = runTest {
        // try body does not throw -> catch skipped, label stays '1'
        "var label='a'; try label='1'; catch (e) label='2'; label".eval().assertEqualsTo("1")
    }

    // try body throws -> catch taken.
    @Test
    fun try_catch_inline_throw() = runTest {
        "var label='a'; try throw 'x'; catch (e) label='2'; label".eval().assertEqualsTo("2")
    }

    // Block try body followed by an optional `;` then `catch`.
    @Test
    fun try_catch_block_with_semicolon() = runTest {
        "var label='a'; try { label='1'; }; catch (e) { label='2'; }; label"
            .eval().assertEqualsTo("1")
    }

    // `try ... ; finally ...` with explicit semicolons.
    @Test
    fun try_finally_inline_with_semicolon() = runTest {
        "var label='a'; try label='1'; finally label='done'; label".eval().assertEqualsTo("done")
    }

    // `try ... ; catch ... ; finally ...` chain with explicit semicolons.
    @Test
    fun try_catch_finally_inline_chain() = runTest {
        "var label='a'; try label='1'; catch (e) label='2'; finally label='fin'; label"
            .eval().assertEqualsTo("fin")
    }

    // The `;`-less try/catch form (valid JS) must keep working.
    @Test
    fun try_catch_inline_no_semicolon() = runTest {
        "var label='a'; try label='1' catch (e) label='2'; label".eval().assertEqualsTo("1")
    }

    // An explicit `;` after a try/catch (or try/finally) block must NOT be swallowed
    // when no continuation keyword follows: the following statements must still run.
    // Regression guard for the fix (keight requires `try` to have catch/finally).
    @Test
    fun try_catch_semicolon_does_not_swallow_following() = runTest {
        "var r=0; try { r=1; } catch(e) {}; r=2; r".eval().assertEqualsTo(2L)
        "var r=0; try { r=1; } finally {}; r=2; r".eval().assertEqualsTo(2L)
    }

    // --- ASI after block statements (V8 alignment) -------------------------
    //
    // A statement that ends with a block-closing '}' may be immediately followed by
    // another statement on the same line without an explicit separator: ECMAScript
    // inserts an implicit ';' (ASI rule #2). keight previously required a literal
    // ';' or newline between them and rejected `} stmt`. These cases must parse and
    // run like V8.

    // try/catch block followed (same line, no ';') by another statement.
    @Test
    fun try_catch_block_followed_by_statement_asi() = runTest {
        "var e; try { throw 1; } catch(err){ e = err; } e".eval().assertEqualsTo(1L)
    }

    // try/catch whose body throws, then `e.stack` is read (the original report).
    @Test
    fun try_catch_block_followed_by_stack_read_asi() = runTest {
        val stack = "let e; try { throw new Error('x'); } catch(err){ e = err; } e.stack"
            .eval().toString()
        assertTrue(stack.contains("Error: x"), "expected stack to mention the error, got: $stack")
    }

    // if-block followed by a statement without separator.
    @Test
    fun if_block_followed_by_statement_asi() = runTest {
        "if (1) { 2; } 3".eval().assertEqualsTo(3L)
    }

    // while/for/do blocks followed by a statement without separator.
    @Test
    fun loop_block_followed_by_statement_asi() = runTest {
        "while (0) { 1; } 5".eval().assertEqualsTo(5L)
        "for (;;) { break; } 7".eval().assertEqualsTo(7L)
        "do { 1; } while (false) 9".eval().assertEqualsTo(9L)
    }

    // Nested blocks: the ASI boundary applies at every level.
    @Test
    fun nested_block_followed_by_statement_asi() = runTest {
        "try { if (1) { 2; } 3; } catch(e) { 4; } 6".eval().assertEqualsTo(6L)
    }

    // ASI must NOT apply inside object literals: members are comma-separated, so
    // `{ a(){} b(){} }` stays a SyntaxError (matching V8).
    @Test
    fun object_literal_members_still_require_comma() = runTest {
        assertFailsWith<Throwable> {
            "const o = { a(){} b(){} };".eval()
        }
    }

    // ASI must NOT turn two adjacent *expression statements* into separate
    // statements: only block/control statements ending in '}' or a control ')'
    // (e.g. do/while) permit a following statement without a separator. These all
    // stay SyntaxErrors, matching V8.
    @Test
    fun expression_statement_adjacency_still_errors() = runTest {
        for (src in listOf("a b", "1 2", "a() b()", "(1) 2", "x = 1 y = 2")) {
            assertFailsWith<Throwable>("expected '$src' to be a SyntaxError") {
                src.eval()
            }
        }
    }
}
