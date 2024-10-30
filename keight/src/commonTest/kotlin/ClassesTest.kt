import io.github.alexzhirkevich.keight.js.JSClass
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

//@Ignore
class ClassesTest {

    @Test
    fun declaration() = runTest {
        """
           class Test {
                foo = 3
           } 

           new Test().foo
        """.trimIndent().eval().assertEqualsTo(3L)

        """
           class Test {
               constructor(x){
                   this.x = x
               }
           }
            
           let t = new Test(123)
           t.x
        """.trimIndent().eval().assertEqualsTo(123L)
    }

    @Test
    fun singleConstructor() = runTest {
        assertFailsWith<SyntaxError> {
            """
               class Test {
                   constructor(){}
                   constructor(x){
                       this.x = x
                   }
               }
            """.trimIndent().eval()
        }
    }

    @Test
    fun methods() = runTest {
        """
           class Test {
               method(x){
                   return x
               }
           }
            
           let t = new Test()
           t.method(123)
        """.trimIndent().eval().assertEqualsTo(123L)

        """
             class Test  {
                 method(){ return 1 }
                 method(){ return 2 }
             }
             
             (new Test()).method()
        """.trimIndent().eval().assertEqualsTo(2L)
    }

    @Test
    fun inlineMethodInvoke() = runTest {
        """
           class Test {
               method(x){
                   return x
               }
           }
            
           new Test().method(123)
        """.trimIndent().eval().assertEqualsTo(123L)
    }

    @Test
    fun inheritance() = runTest {
        """
           class A {
               a(){ return 'a'}
           }
           class B extends A {}
            
           new B().a()
        """.trimIndent().eval().assertEqualsTo("a")

        assertTrue {
            """
               class A extends Object {
                   a(){ return 'a'}
               }
               let a = new A()
            """.trimIndent().eval() is JSObject
        }
    }

    @Test
    fun instanceof() = runTest {

        assertTrue {
            """
               class Test {}
               let t = new Test()
               t instanceof Test
             """.trimIndent().eval() as Boolean
        }

        assertTrue {
            """
               class A {}
               class B extends A {}
                
               new B() instanceof A
            """.trimIndent().eval() as Boolean
        }

        assertTrue {
            """
               class Test {}
               let t = new Test()
               t instanceof Object
             """.trimIndent().eval() as Boolean
        }

    }

    @Test
    fun superConstructorAndMethod() = runtimeTest { runtime ->

        """
            class Person {
                constructor(name) {
                    this.name = name;
                }
                getDetails() {
                    return this.name
                }
            }
            
            class Employee extends Person {
                constructor(name, company) {
                    super(name);
                    this.company = company;
                }
            }
            
            const emp1 = new Employee("John", "Unilever");
        """.trimIndent().eval(runtime)

        "emp1.getDetails()".eval(runtime).assertEqualsTo("John")
        "emp1.name".eval(runtime).assertEqualsTo("John")
        "emp1.company".eval(runtime).assertEqualsTo("Unilever")
    }

    @Test
    fun override() = runTest {

        """
            class A {
                test() {
                    return 'A'
                }
            }
            
            class B extends A {
                 test() {
                    return 'B'
                }
            }
            
            new B().test()
        """.trimIndent().eval().assertEqualsTo("B")
    }

    @Test
    fun static() = runtimeTest { runtime ->

        """
            class A {
                static test = 'static'
                
                static method(){
                    return A.test
                }
            }            
        """.trimIndent().eval(runtime)

        "A.test".eval(runtime).assertEqualsTo("static")
        "A.method()".eval(runtime).assertEqualsTo("static")
    }

    @Test
    fun staticInheritance()= runtimeTest { runtime ->

        """
            class A {
                static test = 'static'
                
                static method(){
                    return A.test
                }
            }   
             
            class B extends A {}
        """.trimIndent().eval(runtime)

        "B.test".eval(runtime).assertEqualsTo("static")
        "B.method()".eval(runtime).assertEqualsTo("static")
    }


    @Test
    fun doubleSuperCall() = runTest {

        assertFailsWith<ReferenceError> {
            """
            class A {
                constructor() {
                }
            }
            
            class B extends A {
                constructor(x) {
                    super();
                    super();
                    this.x = x
                }
            }
            
            let b = new B()
            
            """.trimIndent().eval()
        }
    }

    @Test
    @Ignore
    fun missedSuperCall() = runTest {
        assertFailsWith<ReferenceError> {
            """
                class A {
                }
                    
                class B extends A {
                    constructor(x) {
                        console.log(this)
                        this.x = x;
                    }
                }
                
                const emp1 = new B(1);
            """.trimIndent().eval()
        }
    }
    @Test
    fun notMatchingArgConstructor()= runTest {
        """
            class Test {
                constructor(x){
                    this.x = x
                    if (x !== undefined)
                        throw "error"
                }
            } 
            
            let t = new Test()
            t.x
        """.trimIndent().eval().assertEqualsTo(Unit)
    }

    @Test
    fun number() = runTest {
        "1 instanceof Number".eval().assertEqualsTo(false)
        "new Number(1)".eval().assertEqualsTo(1L)
        "new Number('1')".eval().assertEqualsTo(1L)
        "new Number(1) instanceof Number".eval().assertEqualsTo(true)
        "new Number(1) === Number(1)".eval().assertEqualsTo(false)
        "new Number('1') == Number(true)".eval().assertEqualsTo(true)
        "new Number('2') > Number(true)".eval().assertEqualsTo(true)
    }
}