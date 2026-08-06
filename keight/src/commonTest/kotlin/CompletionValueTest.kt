import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Completion-value propagation for statement-level control structures (Issue #23).
 *
 * In ECMAScript the completion value of the last statement of a program is its value.
 * keight previously returned `Undefined` for *every* `for`/`for-in`/`for-of`/`while`/`do-while`
 * and `switch` statement used as a program's final statement, discarding the last evaluated
 * expression. These tests pin the spec-compliant behavior now that the loops and `OpSwitch`
 * propagate their inner completion value.
 *
 * Reference (Node 22):
 *   for (var i=0;i<3;i++) i            => 2
 *   for (var i=0;i<10;i++){if(i==3)break;i} => 2
 *   for (var k in {x:1}) k             => 'x'
 *   for (const x of [1,2,3]) x         => 3
 *   while(i<3){i++}                     => 2
 *   do {i++} while(i<3)                 => 2
 *   while(false) 1                      => undefined
 *   for(;;){}                           => undefined
 *   switch(1){case 1:'x'}               => 'x'
 *   switch(1){case 1:'x';break}         => 'x'
 *   switch(2){case 1:'x';default:'y'}   => 'y'
 *   switch(1){case 1:'x';break;default:'y'} => 'x'
 *   switch(1){case 1:'a';default:'b'}   => 'b'
 *   try{'a'}catch(e){'b'}               => 'a'
 *   try{throw 1}catch(e){'b'}           => 'b'
 *   try{'a'}finally{'f'}                => 'a'
 *   try{throw 1}catch(e){'b'}finally{'f'} => 'b'
 *   try{}catch(e){}                     => undefined
 */
class CompletionValueTest {

    @Test
    fun for_last_body_value() = runTest {
        """var r = 0; for (var i = 0; i < 3; i++) r = i; r""".trimIndent().eval().assertEqualsTo(2L)
        // The completion value itself (last body expression) is the final `i` value.
        """for (var i = 0; i < 3; i++) i""".trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun for_break_keeps_last_value() = runTest {
        """for (var i = 0; i < 10; i++) { if (i == 3) break; i }""".trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun forIn_last_key_value() = runTest {
        """var r = ''; for (var k in {x: 1}) r = k; r""".trimIndent().eval().assertEqualsTo("x")
        """for (var k in {x: 1}) k""".trimIndent().eval().assertEqualsTo("x")
    }

    @Test
    fun forOf_last_value() = runTest {
        """var r = 0; for (const x of [1, 2, 3]) r = x; r""".trimIndent().eval().assertEqualsTo(3L)
        """for (const x of [1, 2, 3]) x""".trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun while_last_body_value() = runTest {
        """var i = 0; while (i < 3) { i++ }""".trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun doWhile_last_body_value() = runTest {
        """var i = 0; do { i++ } while (i < 3)""".trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun while_false_is_undefined() = runTest {
        """while (false) 1""".trimIndent().eval().assertEqualsTo(Undefined)
    }

    @Test
    fun empty_loops_are_undefined() = runTest {
        // NOTE: `for (;;) {}` is a genuine infinite loop in ECMAScript (the omitted condition
        // is `true`), and keight has no step limit, so it can never complete — it must not be
        // asserted here. `do {} while (false)` runs exactly once and completes with `undefined`,
        // which is the finite analogue of the "empty loop body" case.
        """do {} while (false)""".trimIndent().eval().assertEqualsTo(Undefined)
    }

    @Test
    fun switch_case_completion_value() = runTest {
        """switch (1) { case 1: 'x' }""".trimIndent().eval().assertEqualsTo("x")
    }

    @Test
    fun switch_case_with_break_keeps_value() = runTest {
        // break must not discard the case-body completion value
        """switch (1) { case 1: 'x'; break }""".trimIndent().eval().assertEqualsTo("x")
    }

    @Test
    fun switch_default_completion_value() = runTest {
        """switch (2) { case 1: 'x'; default: 'y' }""".trimIndent().eval().assertEqualsTo("y")
    }

    @Test
    fun switch_break_prevents_default() = runTest {
        """switch (1) { case 1: 'x'; break; default: 'y' }""".trimIndent().eval().assertEqualsTo("x")
    }

    @Test
    fun switch_fallthrough_completion_value() = runTest {
        """switch (1) { case 1: 'a'; default: 'b' }""".trimIndent().eval().assertEqualsTo("b")
    }

    @Test
    fun try_block_completion_value() = runTest {
        """try { 'a' } catch (e) { 'b' }""".trimIndent().eval().assertEqualsTo("a")
        """try { 1; 2; 3 } catch (e) { 'b' }""".trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun catch_block_completion_value() = runTest {
        """try { throw 1 } catch (e) { 'b' }""".trimIndent().eval().assertEqualsTo("b")
        """try { throw new Error('x') } catch (e) { e.message }""".trimIndent().eval().assertEqualsTo("x")
    }

    @Test
    fun finally_value_is_discarded() = runTest {
        """try { 'a' } finally { 'f' }""".trimIndent().eval().assertEqualsTo("a")
        """try { 'a' } catch (e) { 'b' } finally { 'f' }""".trimIndent().eval().assertEqualsTo("a")
        """try { throw 1 } catch (e) { 'b' } finally { 'f' }""".trimIndent().eval().assertEqualsTo("b")
    }

    @Test
    fun empty_try_is_undefined() = runTest {
        """try { } catch (e) { }""".trimIndent().eval().assertEqualsTo(Undefined)
        """try { } finally { 'f' }""".trimIndent().eval().assertEqualsTo(Undefined)
    }

    @Test
    fun nested_try_completion_value() = runTest {
        """try { try { throw 1 } catch (e) { 'inner' } } catch (e) { 'outer' }"""
            .trimIndent().eval().assertEqualsTo("inner")
    }

    @Test
    fun try_inside_control_structures() = runTest {
        """if (true) { try { 'a' } catch (e) { 'b' } }""".trimIndent().eval().assertEqualsTo("a")
        """for (var i = 0; i < 2; i++) { try { i } catch (e) { 'b' } }""".trimIndent().eval().assertEqualsTo(1L)
    }
}
