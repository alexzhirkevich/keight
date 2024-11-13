import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.JSRuntime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjectTest {

    @Test
    fun context() = runtimeTest { runtime ->

        assertTrue {
            "const person = {}; person".eval() is JSObject
        }

        assertTrue {
            "function x(obj) { return obj }; x({})".eval() is JSObject
        }

        """
            function x() { 
                return { test: 1 }
            }
            x().test
        """.eval().assertEqualsTo(1L)

        val obj = "{ name : 'test', x : 1 }".eval(runtime) as JSObject

        runtime.toKotlin(obj.get("name", runtime)).assertEqualsTo("test")
        runtime.toKotlin(obj.get("x",runtime)).assertEqualsTo(1L)


        "typeof {}".eval().assertEqualsTo("object")
        "let x = {}; typeof x".eval().assertEqualsTo("object")
        "let x = ({}); typeof x".eval().assertEqualsTo("object")
        "let x = Object({}); typeof x".eval().assertEqualsTo("object")
        "let x = 1; if ({}) { x = 2 }; x".eval().assertEqualsTo(2L)
        """
            function test(x) { 
                return x
            }
            typeof test({})
        """.trimIndent().eval().assertEqualsTo("object")
    }

    @Test
    fun syntax() = runtimeTest { runtime ->

        """
            let obj = {
                string : "string",
                number : 123,
                f : function() { },
                af : () => {}
            } 
        """.trimIndent().eval(runtime)

        "typeof(obj.string)".eval(runtime).assertEqualsTo("string")
        "typeof(obj.number)".eval(runtime).assertEqualsTo("number")
        "typeof(obj.f)".eval(runtime).assertEqualsTo("function")
        "typeof(obj.af)".eval(runtime).assertEqualsTo("function")
        "typeof(obj.nothing)".eval(runtime).assertEqualsTo("undefined")
    }

    @Test
    fun getters() = runTest {
        "let obj = { name : 'string' }; obj['name']".eval().assertEqualsTo("string")
        "let obj = { name : 'string' }; obj.name".eval().assertEqualsTo("string")
    }

    @Test
    fun setters() = runTest {
        "let obj = {}; obj['name'] = 213; obj.name".eval().assertEqualsTo(213L)
        "let obj = {}; obj.name = 213; obj.name".eval().assertEqualsTo(213L)
    }

    @Test
    fun object_entries_keys() = runTest {
        "typeof Object".eval().assertEqualsTo("function")

        "Object.keys({ name : 'test' })".eval().assertEqualsTo(listOf("name"))
        "Object.keys({ name : 'test', x : 1 })".eval().assertEqualsTo(listOf("name", "x"))
        "Object.keys([1,2,3])".eval().assertEqualsTo(listOf("0","1","2"))
        ("Object.keys(1)".eval() as List<*>).size.assertEqualsTo(0)

        "Object.entries({ name : 'test' })".eval()
            .assertEqualsTo(listOf(listOf("name", "test")))
        "Object.entries({ name : 'test', x : 1 })".eval()
            .assertEqualsTo(listOf(listOf("name", "test"), listOf("x", 1L)))
        ("Object.entries(1)".eval() as List<*>).size.assertEqualsTo(0)
    }

    @Test
    fun object_prototype() {
        """
            function Person(name) {
                this.name = name
            }
            
            let person = new Person('John')
            
            Object.getPrototypeOf(person) == person.prototype
        """.trimIndent()
    }

    @Test
    fun contains() = runtimeTest { runtime ->
        "let obj = { name : 'test'}".eval(runtime)
        assertTrue { "'name' in obj".eval(runtime) as Boolean }
        assertFalse { "'something' in obj".eval(runtime) as Boolean }
    }

    @Test
    fun assign() = runtimeTest { runtime ->
        """
            // Create Target Object
            const person1 = {
                firstName: "John",
                lastName: "Doe",
                age: 50,
                eyeColor: "blue"
              };
              
           // Create Source Object
            const person2 = {firstName: "Anne",lastName: "Smith"};

            // Assign Source to Target
            Object.assign(person1, person2);
        """.eval(runtime)

        "person1.firstName".eval(runtime).assertEqualsTo("Anne")
        "person1.lastName".eval(runtime).assertEqualsTo("Smith")
        "person1.age".eval(runtime).assertEqualsTo(50L)
    }

    @Test
    fun contextual_increment() = runtimeTest {
        "let obj = { x : 0 }".eval(it)
        "obj.x++; obj.x".eval(it).assertEqualsTo(1L)
        "obj.x+=1; obj.x".eval(it).assertEqualsTo(2L)
        "obj['x']++; obj.x".eval(it).assertEqualsTo(3L)
        "obj['x']+=1; obj.x".eval(it).assertEqualsTo(4L)
    }
}