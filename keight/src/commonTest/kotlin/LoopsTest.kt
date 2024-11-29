import io.github.alexzhirkevich.keight.js.ReferenceError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LoopsTest {

    @Test
    fun whileLoop() = runTest {
        """
            var x = 0
            while(x != 3) {
                x += 1
            }
            x
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            var x = 0
            while(x < 3)
                x += 1
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun doWhileLoop()= runTest  {
        """
            var x = 0
            do {
                x+=1
            } while(x != 3)
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun forLoop()= runTest {
        """
            var x = 0
            for(let i = 0; i<3;i++){
                x+=1
            }
            x
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            let i = 0
            for(i = 0; i<3;i++){
                
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            let i = 0
            for(i = 0; i<3;){
                i++
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            let i = 0
            for(; i<3;){
                i++
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            let i = 0
            for(;;){
                i++
                if (i >= 3) 
                    break
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            for(i = 0; i<3;i++){
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)

        """
            var x = 0
            for(let i = 0; i<3;i++){
                let y = 1 // redeclare
                "test" /**/
                x+=1
            }
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun for_in_loop() = runTest {
        """
            var x = [2, 4, 8]
            var sum = 0
            for(let i in x){
               sum += x[i]
            }
            sum
        """.trimIndent().eval().assertEqualsTo(14L)

        """
            var x = { a: 1, b: 2, c : 3}
            var sum = 0
            for(let i in x){
               sum += x[i]
            }
            sum
        """.eval().assertEqualsTo(6L)


        """
            for(i in [2, 4, 8]){
            }
            i
        """.trimIndent().eval().assertEqualsTo("2")

        assertFailsWith<ReferenceError> {
            """
                for(let i in [2, 4, 8]) {   }
                console.log(i)
            """.eval()
        }
    }

    @Test
    fun multiple_expressions()= runTest {
        """
            let a;
            let b
            for(a = 0, b = 2; a + b < 10; a++,b++){
                
            }
            a + b
        """.trimIndent().eval().assertEqualsTo(10L)

        """
            var i = 0;
            while(false, i<3){
                i++
            }
            i
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun early_return()= runTest {
        """
            var x = 0
            for(let i = 0; i<3;i++){
                break
                x+=1
            }
            x
        """.trimIndent().eval().assertEqualsTo(0L)

        """
            var x = 0
            for(let i = 0; i<3;i++){
                if (i % 2 == 1)
                    continue
                x+=1
            }
            x
        """.trimIndent().eval().assertEqualsTo(2L)

        """
            var i = 0
            var x = 0
            while(x < 3) {
                i++
                if (i == 1)
                    break
                x += 1
            }
            i
        """.trimIndent().eval().assertEqualsTo(1L)

        """
            var i = 0
            var x = 0
             do {
                i++
                if (i % 2 == 1)
                    continue
                x += 1
            } while(x < 3)
            i
        """.trimIndent().eval().assertEqualsTo(6L)

        """
            var i = 0
            var x = 0
            while(x < 3) {
                i++
                if (i == 1)
                    break
                x += 1
            }
            i
        """.trimIndent().eval().assertEqualsTo(1L)

        """
            var i = 0
            var x = 0
            do {
                i++
                if (i % 2 == 1)
                    continue
                x += 1
            } while(x < 3)
            i
        """.trimIndent().eval().assertEqualsTo(6L)
    }

    @Test
    fun scopes() = runTest {
        assertFailsWith<ReferenceError> {
            """
            let i = 1
            while (i<3){
                let x = 1
                i++;
            }
            x
        """.trimIndent().eval()
        }

        assertFailsWith<ReferenceError> {
            """
            let i = 1
            do {
                let x = 1
                i++;
            } while (i<3)
            x
        """.trimIndent().eval()
        }

        assertFailsWith<ReferenceError> {
            """
            let i = 1
            for (i=0; i<3; i++){
                let x = 1
            }
            x
        """.trimIndent().eval()
        }
    }
}