import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that source-line and source-column numbers reported in stack traces
 * stay consistent with the actual source layout regardless of surrounding
 * whitespace: blank lines, leading/trailing spaces, tabs, comments and mixed
 * indentation.
 *
 * The reported line must equal the physical line (within the trimmed script)
 * where the error is *constructed* (`new Error`). The reported column must
 * point at the `new` keyword (matching V8), not at the enclosing statement
 * keyword.
 */
class SourceLocationWhitespaceTest {

    /**
     * Extract the deepest frame's `:LINE:COL` from a stack string.
     */
    private fun lineColumn(stack: String): Pair<Int, Int> {
        val frame = stack.lines().firstOrNull { it.contains("at ") }
            ?: error("no stack frame in:\n$stack")
        val m = Regex(""":(\d+):(\d+)""").find(frame)
            ?: error("no :LINE:COL in frame: $frame")
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private suspend fun stackOf(script: String): String =
        script.trimIndent().eval().toString()

    /** Physical line (1-based) of `new Error` inside the trimmed script. */
    private fun expectedLine(script: String): Int =
        script.trimIndent().lines().indexOfFirst { "new Error" in it } + 1

    /** 1-based column of the `new` keyword on its line. */
    private fun expectedColumn(script: String): Int {
        val line = script.trimIndent().lines().first { "new Error" in it }
        return line.indexOf("new") + 1
    }

    private suspend fun assertLineConsistent(script: String) {
        val (line, _) = lineColumn(stackOf(script))
        val expected = expectedLine(script)
        assertTrue(
            "reported error line $line should equal actual line $expected\n" +
                script.trimIndent()
        ) { line == expected }
    }

    private suspend fun assertColumnConsistent(script: String) {
        val (_, col) = lineColumn(stackOf(script))
        val expected = expectedColumn(script)
        assertTrue(
            "reported error column $col should equal `new` keyword column $expected\n" +
                script.trimIndent()
        ) { col == expected }
    }

    @Test
    fun compactNoBlankLines() = runTest {
        assertLineConsistent(
            """
            function f() {
            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun leadingBlankLines() = runTest {
        assertLineConsistent(
            """


            function f() {

            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun blankLinesInsideBody() = runTest {
        assertLineConsistent(
            """
            function f() {



            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun multipleConsecutiveBlankLines() = runTest {
        assertLineConsistent(
            """
            function f() {




            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun trailingBlankLines() = runTest {
        assertLineConsistent(
            """
            function f() {
            return new Error('x').stack;
            }
            f();



            """.trimIndent()
        )
    }

    @Test
    fun heavySpaceIndentation() = runTest {
        assertLineConsistent(
            """
            function f() {
                return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun errorOnFirstLine() = runTest {
        assertLineConsistent(
            """
            function f() { return new Error('x').stack; }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun errorOnLastLine() = runTest {
        assertLineConsistent(
            """
            function f() {
            const a = 1;
            const b = 2;
            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun singleLineCommentsBeforeError() = runTest {
        assertLineConsistent(
            """
            function f() {
            // a comment
            // another comment
            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun blockCommentBeforeError() = runTest {
        assertLineConsistent(
            """
            function f() {
            /* a block
               comment spanning
               several lines */
            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun tabIndentation() = runTest {
        val tab = "\t"
        val script = "function f() {\n${tab}return new Error('x').stack;\n}\nf();\n"
        assertLineConsistent(script)
    }

    @Test
    fun throwNewErrorStatement() = runTest {
        val script = """
            function f() {
            throw new Error('x');
            }
            let s = null;
            try { f(); } catch (e) { s = e.stack; }
            s;
        """.trimIndent()
        val (line, col) = lineColumn(script.trimIndent().eval().toString())
        assertTrue("throw new Error line should be consistent") {
            line == expectedLine(script)
        }
        assertTrue("throw new Error column should point at `new` ($col)") {
            col == expectedColumn(script)
        }
    }

    @Test
    fun columnPointsAtNewKeywordForReturn() = runTest {
        assertColumnConsistent(
            """
            function f() {
            return new Error('x').stack;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun columnPointsAtNewKeywordForVar() = runTest {
        assertColumnConsistent(
            """
            function f() {
            var e = new Error('x').stack;
            return e;
            }
            f();
            """.trimIndent()
        )
    }

    @Test
    fun asyncAwaitRejectLineConsistent() = runTest {
        val script = """
            async function f() {

            await Promise.reject(new Error('x'));
            }
            let s = null;
            try { await f(); } catch (e) { s = e.stack; }
            s;
        """.trimIndent()
        val (line, _) = lineColumn(script.trimIndent().eval().toString())
        assertTrue("async await error line should be consistent (got $line)") {
            line == expectedLine(script)
        }
    }

    @Test
    fun thenCallbackLineConsistent() = runTest {
        val script = """
            function cb() {

            return new Error('x').stack;
            }
            let s = null;
            s = await Promise.resolve().then(cb).catch(e => e.stack);
            s;
        """.trimIndent()
        val (line, _) = lineColumn(script.trimIndent().eval().toString())
        assertTrue("then-callback error line should be consistent (got $line)") {
            line == expectedLine(script)
        }
    }

    @Test
    fun differentLayoutsReportDifferentLines() = runTest {
        val early = """
            function f() {
            return new Error('x').stack;
            }
            f();
        """.trimIndent()
        val late = """
            function f() {



            return new Error('x').stack;
            }
            f();
        """.trimIndent()
        val (earlyLine, _) = lineColumn(stackOf(early))
        val (lateLine, _) = lineColumn(stackOf(late))
        assertTrue("layouts with blank lines must shift the reported line ($earlyLine vs $lateLine)") {
            earlyLine != lateLine
        }
        assertTrue("early layout must report the earlier line") { earlyLine < lateLine }
    }
}
