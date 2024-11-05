import io.github.alexzhirkevich.keight.js.RangeError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsArrayTest {

    @Test
    fun indexOf() = runtimeTest {
        "const array = [2, 9, 9];".eval(it)
        "array.indexOf(2);".eval(it).assertEqualsTo(0L)
        "array.indexOf(7);".eval(it).assertEqualsTo(-1L)
        "array.indexOf(9, 2);".eval(it).assertEqualsTo(2L)
        "array.indexOf(2, -1);".eval(it).assertEqualsTo(-1L)
        "array.indexOf(2, -3);".eval(it).assertEqualsTo(0L)
    }
    @Test
    fun lastIndexOf() = runtimeTest {
        "const numbers = [2, 5, 9, 2];".eval(it)
        "numbers.lastIndexOf(2);".eval(it).assertEqualsTo(3L)
        "numbers.lastIndexOf(7);".eval(it).assertEqualsTo(-1L)
        "numbers.lastIndexOf(2, 3);".eval(it).assertEqualsTo(3L)
        "numbers.lastIndexOf(2, 2);".eval(it).assertEqualsTo(0L)
        "numbers.lastIndexOf(2, -2);".eval(it).assertEqualsTo(0L)
        "numbers.lastIndexOf(2, -1);".eval(it).assertEqualsTo(3L)

        "[1,2,2,3].lastIndexOf(2)".eval().assertEqualsTo(2L)
        "[1,2,2,3].lastIndexOf(2,1)".eval().assertEqualsTo(1L)
    }

    @Test
    fun forEach() = runTest {
        """
            let x = 0
            let arr = [1,2,3]
            arr.forEach(v => x+=v)
            x
        """.trimIndent().eval().assertEqualsTo(6L)

        """
            let x = 0
            let arr = [1,2,3]
            arr.forEach(() => x++)
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun map()= runTest {
        """
            let arr = [1,2,3]
            arr.map(v => v * v)
        """.trimIndent().eval().assertEqualsTo(listOf(1L,4L,9L))

        """
            let arr = [1,2,3]
            arr.map(() => 1)
        """.trimIndent().eval().assertEqualsTo(listOf(1L,1L,1L))
    }

    @Test
    fun filter()= runTest {
        """
            let arr = [1,2,3,4]
            arr.filter(v => v % 2 === 0)
        """.trimIndent().eval().assertEqualsTo(listOf(2L, 4L))

        """
            let arr = [1,2,3,4]
            arr.filter(() => 1)
        """.trimIndent().eval().assertEqualsTo(listOf(1L,2L,3L,4L))
    }

    @Test
    fun some()= runTest {
        assertTrue {
            """
                let arr = [1,2,3,4]
                arr.some(v => v === 2)
            """.trimIndent().eval() as Boolean
        }

        assertFalse {
            """
                let arr = [1,2,3,4]
                arr.some(v => v === 5)
            """.trimIndent().eval() as Boolean
        }
    }

    @Test
    fun sort()= runTest {
        """
            let arr = [66,2,8]
            arr.sort()
            arr
        """.trimIndent().eval().assertEqualsTo(listOf(2L,8L, 66L))
    }

    @Test
    fun at()= runTest {
        "[66,2,8].at(0)".eval().assertEqualsTo(66L)
        "[66,2,8].at(3)".eval().assertEqualsTo(Unit)

        "[66,2,8][1]".eval().assertEqualsTo(2L)
        "[66,2,8][3]".eval().assertEqualsTo(Unit)
    }

    @Test
    fun copyWithin() = runTest {
        """
            const fruits = ["Banana", "Orange", "Apple", "Mango"];
            fruits.copyWithin(2, 0);
        """.eval().assertEqualsTo(listOf("Banana", "Orange", "Banana", "Orange"))

        """
            const fruits = ["Banana", "Orange", "Apple", "Mango", "Kiwi"];
            fruits.copyWithin(2, 0, 2);
        """.eval().assertEqualsTo(listOf("Banana", "Orange", "Banana", "Orange", "Kiwi"))
    }

    @Test
    fun flat() = runTest {
        "[[1,2],[3,4],[5,6]].flat()".eval().assertEqualsTo(listOf(1L, 2L, 3L, 4L, 5L, 6L))
        "[[1,2],[3,4]].flat(0)".eval().assertEqualsTo(listOf(listOf(1L, 2L), listOf(3L, 4L)))
        "[[[1]],[[2]]].flat()".eval().assertEqualsTo(listOf(listOf(1L), listOf(2L)))
        "[[[1]],[[2]]].flat(2)".eval().assertEqualsTo(listOf(1L, 2L))
    }

    @Test
    fun flatMap() = runTest {
        "[1,2,3].flatMap(x => [x, x+1])".eval().assertEqualsTo(listOf(1L, 2L, 2L, 3L, 3L, 4L))
    }

    @Test
    fun splice() = runTest {
        """
            const fruits = ["Banana", "Orange", "Apple", "Mango"];
            fruits.splice(2, 0, "Lemon", "Kiwi");
            fruits
        """.eval().assertEqualsTo(listOf("Banana", "Orange", "Lemon", "Kiwi", "Apple", "Mango"))

        """
            const fruits = ["Banana", "Orange", "Apple", "Mango"];
            fruits.splice(2, 2, "Lemon", "Kiwi");
        """.eval().assertEqualsTo(listOf("Apple", "Mango"))
    }

    @Test
    fun toSpliced() = runTest {
        """
            const fruits = ["Banana", "Orange", "Apple", "Mango"];
            fruits.toSpliced(2, 0, "Lemon", "Kiwi");
        """.eval().assertEqualsTo(listOf("Banana", "Orange", "Lemon", "Kiwi", "Apple", "Mango"))
    }

    @Test
    fun fun_methods() = runTest {
        assertTrue {
            "Array.isArray([1,2,3])".eval() as Boolean
        }
    }

    @Test
    fun fun_constructor() = runTest {

        "Array(1,2,3)".eval().assertEqualsTo(listOf(1L,2L,3L))
        "new Array(1,2,3)".eval().assertEqualsTo(listOf(1L,2L,3L))

        "Array(2)".eval().assertEqualsTo(listOf(Unit, Unit))
        "Array(2.0)".eval().assertEqualsTo(listOf(Unit, Unit))

        assertFailsWith<RangeError> {
            "Array(2.5)".eval()
        }.message.assertEqualsTo("Uncaught RangeError: Invalid array length")

        assertFailsWith<RangeError> {
            "Array(NaN)".eval()
        }.message.assertEqualsTo("Uncaught RangeError: Invalid array length")

        assertTrue {
            "[1,2,3] instanceof Array".eval() as Boolean
        }
    }
}