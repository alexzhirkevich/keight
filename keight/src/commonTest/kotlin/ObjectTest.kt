import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjectTest {

    @Test
    fun context() = runtimeTest { runtime ->

        assertTrue {
            "const person = {}; person".eval() is JsObject
        }

        assertTrue {
            "function x(obj) { return obj }; x({})".eval() is JsObject
        }

        """
            function x() { 
                return { test: 1 }
            }
            x().test
        """.eval().assertEqualsTo(1L)

        val obj = "{ name : 'test', x : 1 }".eval(runtime) as JsObject

        obj.get("name".js, runtime)?.toKotlin(runtime).assertEqualsTo("test")
        obj.get("x".js,runtime)?.toKotlin(runtime).assertEqualsTo(1L)


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

    @Test
    fun keyword_property_names() = runtimeTest {
        // `default` and other keywords must be usable as object property names
        "var v = { min: 0, max: 100, step: 5, default: 50, unit: '%' }; v.default"
            .eval(it).assertEqualsTo(50L)

        "({ default: 50 }).default".eval(it).assertEqualsTo(50L)
        "({ if: 1, for: 2, class: 3 }).for".eval(it).assertEqualsTo(2L)
        "({ case: 'a', switch: 'b' }).switch".eval(it).assertEqualsTo("b")

        // access via computed and dot notation
        "var o = { default: 7 }; o['default']".eval(it).assertEqualsTo(7L)
        "var o = { return: 9 }; o.return".eval(it).assertEqualsTo(9L)
    }

    @Test
    fun keyword_property_names_nested() = runtimeTest {
        """
            var ui = {
                slider: function(name, opts) { return opts.default; }
            };
            ui.slider("音量", { min: 0, max: 100, step: 5, default: 50, unit: "%" })
        """.trimIndent().eval(it).assertEqualsTo(50L)
    }

    @Test
    fun keyword_method_shorthand() = runtimeTest {
        // keyword as a method-shorthand name must still work
        """
            var o = { default() { return 11 } };
            o.default()
        """.trimIndent().eval(it).assertEqualsTo(11L)
    }

    @Test
    fun async_method_still_parses() = runtimeTest {
        // `async` in object context must remain a modifier, not a property name
        """
            const obj = {
                async getValue() { return 42 }
            }
            await obj.getValue()
        """.trimIndent().eval(it).assertEqualsTo(42L)
    }
}