import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.Undefined
import kotlin.test.Test
import kotlin.test.assertFailsWith

class IteratorTest {

    @Test
    fun basic() = runtimeTest {
        "let iter = [1,2,3].values()".eval(it)
        "let x = iter.next()".eval(it)

        "x.value".eval(it).assertEqualsTo(1L)
        "x.done".eval(it).assertEqualsTo(false)

        "x = iter.next()".eval(it)

        "x.value".eval(it).assertEqualsTo(2L)
        "x.done".eval(it).assertEqualsTo(false)

        "x = iter.next()".eval(it)

        "x.value".eval(it).assertEqualsTo(3L)
        "x.done".eval(it).assertEqualsTo(false)

        "x = iter.next()".eval(it)

        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)
    }

    @Test
    fun symbol_call() = runtimeTest {
        "let arr = [1,2,3]".eval(it)
        "let iterator = arr[Symbol.iterator]".eval(it)

        "iterator.call(arr).next().value".eval(it).assertEqualsTo(1L)
        "arr[Symbol.iterator]().next().value".eval(it).assertEqualsTo(1L)
    }

    @Test
    fun forEach() = runtimeTest {
        "let iter = [1,2,3].values()".eval(it)
        """
            var x = 0
            var idx = 0
            iter.forEach((v,i) => { x+=v; idx+=i})
        """.eval(it)
        "x".eval(it).assertEqualsTo(6L)
        "idx".eval(it).assertEqualsTo(3L)


        "iter = [].values()".eval(it)
        """
            var called = false
            iter.forEach(() => called = true)
            called
        """.eval(it).assertEqualsTo(false)
    }

    @Test
    fun filter_ends_with_undefined() = runtimeTest {
        "let iter = [1,2,3,4,5].values()".eval(it)
        "let helper = iter.filter(x => x % 2 == 0)".eval(it)

        "let n = helper.next()".eval(it)
        "let n2 = iter.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(2L)
        "n.done".eval(it).assertEqualsTo(false)
        "n2.value".eval(it).assertEqualsTo(3L)
        "n2.done".eval(it).assertEqualsTo(false)

        "n = helper.next()".eval(it)
        "n2 = iter.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(4L)
        "n.done".eval(it).assertEqualsTo(false)
        "n2.value".eval(it).assertEqualsTo(5L)
        "n2.done".eval(it).assertEqualsTo(false)

        "n = helper.next()".eval(it)
        "n2 = iter.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(Undefined)
        "n.done".eval(it).assertEqualsTo(true)
        "n2.value".eval(it).assertEqualsTo(Undefined)
        "n2.done".eval(it).assertEqualsTo(true)
    }

    @Test
    fun filter_ends_with_value() = runtimeTest {
        "let iter = [1,2,3,4].values()".eval(it)
        "let helper = iter.filter(x => x % 2 == 0)".eval(it)

        "let n = helper.next()".eval(it)
        "let n2 = iter.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(2L)
        "n.done".eval(it).assertEqualsTo(false)
        "n2.value".eval(it).assertEqualsTo(3L)
        "n2.done".eval(it).assertEqualsTo(false)

        "n = helper.next()".eval(it)
        "n2 = iter.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(4L)
        "n.done".eval(it).assertEqualsTo(false)
        "n2.value".eval(it).assertEqualsTo(Undefined)
        "n2.done".eval(it).assertEqualsTo(true)

        "n = helper.next()".eval(it)
        "n.value".eval(it).assertEqualsTo(Undefined)
        "n.done".eval(it).assertEqualsTo(true)
    }

    @Test
    fun every() = runtimeTest {
        "let iter = [1,2,3,4].values()".eval(it)
        "iter.every(x => x % 2 == 0)".eval(it).assertEqualsTo(false)
        "iter.next().value".eval(it).assertEqualsTo(2L)

        "iter = [2,4,5,6].values()".eval(it)
        "iter.every(x => x % 2 == 0)".eval(it).assertEqualsTo(false)
        "iter.next().value".eval(it).assertEqualsTo(6L)

        "iter = [2,4,6,8].values()".eval(it)
        "iter.every(x => x % 2 == 0)".eval(it).assertEqualsTo(true)
        "iter.next().value".eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun some() = runtimeTest {
        "let iter = [1,2,3,4].values()".eval(it)
        "iter.some(x => x % 2 == 0)".eval(it).assertEqualsTo(true)
        "iter.next().value".eval(it).assertEqualsTo(3L)

        "iter = [2,3,4,5].values()".eval(it)
        "iter.some(x => x % 2 == 0)".eval(it).assertEqualsTo(true)
        "iter.next().value".eval(it).assertEqualsTo(3L)

        "iter = [2,4,6,8].values()".eval(it)
        "iter.some(x => x % 2 == 1)".eval(it).assertEqualsTo(false)
        "iter.next().value".eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun map() = runtimeTest {
        "let iter = [1,2,3,4].values()".eval(it)
        "let mapped = iter.map(x => x * 2)".eval(it)
        "mapped.next().value".eval(it).assertEqualsTo(2L)
        "mapped.next().value".eval(it).assertEqualsTo(4L)
        "mapped.next().value".eval(it).assertEqualsTo(6L)
        "mapped.next().value".eval(it).assertEqualsTo(8L)
        "mapped.next().value".eval(it).assertEqualsTo(Undefined)
        "iter.next().value".eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun flatMap() = runtimeTest {
        "let iter = [1,2,3].values()".eval(it)
        "let mapped = iter.flatMap(x => [x,x])".eval(it)
        "mapped.next().value".eval(it).assertEqualsTo(1L)
        "mapped.next().value".eval(it).assertEqualsTo(1L)
        "mapped.next().value".eval(it).assertEqualsTo(2L)
        "mapped.next().value".eval(it).assertEqualsTo(2L)
        "mapped.next().value".eval(it).assertEqualsTo(3L)
        "mapped.next().value".eval(it).assertEqualsTo(3L)
        "mapped.next().value".eval(it).assertEqualsTo(Undefined)
        "iter.next().value".eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun find() = runtimeTest {
        "let iter = [1,9,3,4,1,2,11,4].values()".eval(it)
        "iter.find(x => x % 2 == 0)".eval(it).assertEqualsTo(4L)
        "iter.next().value".eval(it).assertEqualsTo(1L)
        "iter.find(x => x % 2 == 0)".eval(it).assertEqualsTo(2L)
        "iter.next().value".eval(it).assertEqualsTo(11L)

        "iter = [1,2,3].values()".eval(it)
        "iter.find(x => x >3)".eval(it).assertEqualsTo(Undefined)
        "let x = iter.next()".eval(it)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)

    }

    @Test
    fun drop() = runtimeTest {
        "let iter = [1,2,3,4,5].values()".eval(it)
        "let drop = iter.drop(3)".eval(it)
        "drop.next().value".eval(it).assertEqualsTo(4L)
        "drop.next().value".eval(it).assertEqualsTo(5L)
        "let x = drop.next()".eval(it)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)

        "iter = [1,2,3].values()".eval(it)
        "drop = iter.drop(4)".eval(it)
        "x = drop.next()".eval(it)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)
    }

    @Test
    fun take() = runtimeTest {
        "let iter = [1,2,3,4,5].values()".eval(it)
        "let take = iter.take(3)".eval(it)
        "take.next().value".eval(it).assertEqualsTo(1L)
        "take.next().value".eval(it).assertEqualsTo(2L)
        "take.next().value".eval(it).assertEqualsTo(3L)
        "let x = take.next()".eval(it)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)

        "iter = [1,2,3].values()".eval(it)
        "take = iter.take(0)".eval(it)
        "x = take.next()".eval(it)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)

        "iter = [1,2,3].values()".eval(it)
        "take = iter.take(4)".eval(it)
        "take.next().value".eval(it).assertEqualsTo(1L)
        "take.next().value".eval(it).assertEqualsTo(2L)
        "take.next().value".eval(it).assertEqualsTo(3L)
        "x.value".eval(it).assertEqualsTo(Undefined)
        "x.done".eval(it).assertEqualsTo(true)
    }

    @Test
    fun toArray() = runtimeTest {
        "let iter = [1,2,3,4,5].values()".eval(it)
        "iter.toArray()".eval(it).assertEqualsTo(listOf(1L, 2L, 3L, 4L, 5L))
    }
}