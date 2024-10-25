import kotlin.test.Test
import kotlin.test.assertContentEquals


class AssignTest {

    @Test
    fun add_sub_mull_div_assign() {
//
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
    fun increment_decrement() {
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
}
