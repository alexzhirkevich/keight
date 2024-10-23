import kotlin.test.Test

class SwitchTest {

    @Test
    fun test(){
        """
            var x
            switch(3){
                case 1:
                    break
                case 2:
                    break
                case 3:
                    x = 3
                    break
                default:
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun multi_choice(){
        """
            var x
            switch(3){
                case 1:
                case 2:
                case 3:
                    x = 1
                    break
                case 4:
                    x = 2
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo(1L)
    }

    @Test
    fun default_missplacement(){
        """
            var x = ''
            switch(4){
                case 1:
                    x += '1'
                    break
                default:
                    break;
                case 2:
                    x += '2'
                    break
                case 3:
                    x += '3'
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo("")
    }
}