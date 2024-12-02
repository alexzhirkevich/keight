import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith


class AssignTest {

    @Test
    fun add_sub_mull_div_assign() = runTest {
        "var x = 13; x += 17".eval().assertEqualsTo(30L)
        "var x = 56; x -=17".eval().assertEqualsTo(39L)
        "var x = 5; x *=2".eval().assertEqualsTo(10L)
        "var x = 13; x *= -2*2".eval().assertEqualsTo(-52L)
        "var x = 144; x /=6".eval().assertEqualsTo(24L)

        "var x = []; x[0] = 5; x[1] = 10; x[(5-5)] += 10-3; x[5-4] += (4*2); x"
            .eval().let { assertContentEquals(it as Iterable<*>, listOf(12L, 18L)) }

        "var x = []\n x[0] = 5\n x[1] = 10\n x[(5-5)] += 10-3\n x[5-4] += (4*2); x"
            .eval().let { assertContentEquals(it as Iterable<*>, listOf(12L, 18L)) }
    }

    @Test
    fun increment_decrement() = runTest {
        "let x = 5; x++".eval().assertEqualsTo(5L)
        "let x = 5; ++x".eval().assertEqualsTo(6L)

        "let x = 5; x--".eval().assertEqualsTo(5L)
        "let x = 5; --x".eval().assertEqualsTo(4L)

        "let x = 0; let y = x++; y".eval().assertEqualsTo(0L)
        "let x = 0; let y = x++; x".eval().assertEqualsTo(1L)
        "let x = 0; let y = ++x; y".eval().assertEqualsTo(1L)
        "let x = 0; let y = ++x; x".eval().assertEqualsTo(1L)

        "let x = 1; let y = x--; y".eval().assertEqualsTo(1L)
        "let x = 1; let y = x--; x".eval().assertEqualsTo(0L)
        "let x = 1; let y = --x; y".eval().assertEqualsTo(0L)
        "let x = 1; let y = --x; x".eval().assertEqualsTo(0L)
    }

    @Test
    fun destruction_array() = runtimeTest {
        "var [a,b,c] = [1,2,3,4]".eval(it)
        "a".eval(it).assertEqualsTo(1L)
        "b".eval(it).assertEqualsTo(2L)
        "c".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun destruction_object() = runtimeTest {
        "var {a,b,c} = { a : 1, b : 2, c : 3}".eval(it)
        "a".eval(it).assertEqualsTo(1L)
        "b".eval(it).assertEqualsTo(2L)
        "c".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun destruction_combined() = runtimeTest {
        "var { a, x: [b,c] } = { a : 1, x : [2,3] }".eval(it)
        "a".eval(it).assertEqualsTo(1L)
        "b".eval(it).assertEqualsTo(2L)
        "c".eval(it).assertEqualsTo(3L)
    }

    @Test
    fun destruction_into_object() = runtimeTest {
        "var obj = {}; [obj.a, obj['b'], c] = [1,2,3,4]".eval(it)
        "obj.a".eval(it).assertEqualsTo(1L)
        "obj.b".eval(it).assertEqualsTo(2L)
        "c".eval(it).assertEqualsTo(3L)

        assertFailsWith<SyntaxError> {
            "var obj = {}; { obj.a } = { a: 1}".eval()
        }
        assertFailsWith<SyntaxError> {
            "var obj = {}; { obj['a'] } = { a: 1 }".eval()
        }
    }

    @Test
    fun destruction_rest() = runtimeTest {
        "var [ a, ...b ] = [1, 2, 3]".eval(it)
        "a".eval(it).assertEqualsTo(1L)
        "b".eval(it).assertEqualsTo(listOf(2L, 3L))
    }
}
