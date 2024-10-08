package happy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeCheckerTest {

    @BeforeEach
    fun setUp() {
        resetContext()
    }

    @Test
    fun intLiteral() {
        assertType("123", integer)
    }

    @Test
    fun stringLiteral() {
        assertType("\"it works\"", string)
    }

    @Test
    fun booleanLiteral() {
        assertType("true", boolean)
        assertType("false", boolean)
    }

    @Test
    fun letStatement() {
        exec("")
        assertType("{ var x = 5; x }", integer)
        assertEquals(listOf(), typeErrors)
        assertType("{ var x: String = 5; x }", string)
        assertTypeError(IncompatibleType(string, integer, "1:2-1:19".loc))
        exec("var x: UndeclaredType;")
        assertType("x", nothing)
        assertTypeError(UndeclaredType("UndeclaredType", "1:0-1:21".loc))
    }

    @Test
    fun assignmentStatement() {
        assertType("{ var x = 5; x = 10; x }", integer)
        assertEquals(listOf(), typeErrors)
        assertType("{ var x = 5; x = \"text\"; x }", integer)
        assertTypeError(IncompatibleType(integer, string, "1:13-1:23".loc))
    }

    @Test
    fun functionCall() {
        exec(
            """
            func add(a: Integer, b: Integer): Integer {
              a + b
            }
            """
        )
        assertEquals(listOf(), typeErrors)
        assertType("add(5, 10)", integer)
    }

    @Test
    fun functionOverloading() {
        exec("func combine(a: Integer, b: Integer): Integer { a * b }")
        exec("func combine(a: String, b: String): String { a + b }")
        assertEquals(listOf(), typeErrors)
        assertType("combine(\"wo\", \"rd\")", string)
        assertEquals(listOf(), typeErrors)
        assertType("combine(2, 2)", integer)
        assertEquals(listOf(), typeErrors)
    }

    @Test
    fun functionArgumentTypeCheck() {
        exec(
            """
            func add(a: Integer, b: Integer): Integer {
              a + b
            }
            """
        )
        assertType("add(5, \"not a number\")", integer)
        assertTypeError(IncompatibleType(integer, string, "1:3-1:21".loc))
    }

    @Test
    fun functionReturnTypeCheck() {
        exec(
            """
            func add(a: Integer, b: Integer): Integer {
              "not a number"
            }
            """
        )
        assertTypeError(IncompatibleType(integer, string, "2:12-4:12".loc))
    }

    @Test
    fun enumTypeCheck() {
        exec(
            """
            enum Choice { 'A, Integer }
            var choice: Choice;
            """
        )
        val choiceType = EnumType("Choice", setOf(SymbolType("'A"), integer))
        assertType("choice", choiceType)
        exec("choice = 'A;")
        assertEquals(listOf(), typeErrors)
        exec("choice = 5;")
        assertEquals(listOf(), typeErrors)
        exec("choice = 'B;")
        assertTypeError(IncompatibleType(choiceType, SymbolType("'B"), "1:0-1:11".loc))
    }

    @Test
    fun ifExpression() {
        assertType("if 2 > 1 { 2 } else { 1 }", integer)
        assertType("if 2 > 1 { 2 } else { \"text!\" }", EnumType("Inline", setOf(integer, string)))
        assertType("if 2 > 1 { 2 } else if 3 > 2 { true } else { \"text!\" }", EnumType("Inline", setOf(integer, boolean, string)))
    }

    @Test
    fun functionReturnUnionTypeCheck() {
        exec(
            """
            enum Bunch { Integer, 'One }
            """
        )
        exec(
            """
            func correct(a: Integer): Bunch {
              if a > 3 { 4 }
              else { 'One }
            }
            """
        )
        assertEquals(listOf(), typeErrors)
        exec(
            """
            func wrong(a: Integer): Bunch {
              if a > 3 { 4 }
              else if a > 0 { 'One }
              else { "negative" }
            }
            """
        )
        val bunchType = EnumType("Bunch", setOf(integer, SymbolType("'One")))
        val wrongType = EnumType("Inline", setOf(integer, SymbolType("'One"), string))
        assertEquals(
            listOf<TypeError>(IncompatibleType(bunchType, wrongType, "2:12-6:12".loc)),
            typeErrors
        )
    }

    @Test
    fun functionReturnGenericUnionTypeCheck() {
        exec(
            """
            enum Option<T> { T, 'None }
            """
        )
        exec(
            """
            func returnIfSmall(num: Integer): Option<Integer> {
                if num <= 5 { num } else { 'None }
            }
            """
        )
        assertEquals(listOf(), typeErrors)
    }

    @Test
    fun genericEnumTypeCheck() {
        exec(
            """
            enum Opt<T> { T, 'None }
            var maybe: Opt<Integer>;
            """
        )
        val optType = EnumType("Opt", setOf(integer, SymbolType("'None")))
        assertType("maybe", optType)
        exec("maybe = 5;")
        assertEquals(listOf(), typeErrors)
        exec("maybe = 'None;")
        assertEquals(listOf(), typeErrors)
        exec("maybe = false;")
        assertTypeError(IncompatibleType(optType, boolean, "1:0-1:13".loc))
    }

    @Test
    fun matchExpression() {
        assertType("match 3 { 3: \"three\", 5: \"five\", else: \"dunno\" }", string)
        assertType("match 5 { 3: \"three\", 5: 5, else: \"dunno\" }", EnumType("Inline", setOf(string, integer)))
        assertType("match 7 { 3: \"three\", 5: 5, else: 'None }", EnumType("Inline", setOf(string, integer, SymbolType("'None"))))
    }

    @Test
    fun dataType() {
        exec("data MyData { name: String, age: Integer }")
        val myDataType = DataType("MyData", mapOf("name" to string, "age" to integer))
        assertType("MyData { name: \"Luna\", age: 2 }", myDataType)
        exec("var m = MyData { name: \"Luna\", age: 2 };")
        assertType("m", myDataType)
        assertType("m.name", string)
        assertType("m.age", integer)

        assertType("m.breed", nothing)
        assertTypeError(UndeclaredField("breed", myDataType, "1:1-1:2".loc))

        exec("NotMyData { name: \"Luna\", age: \"2\" };")
        assertTypeError(UndeclaredType("NotMyData", "1:0-1:35".loc))

        exec("MyData { name: \"Luna\", age: \"2\" };")
        assertTypeError(IncompatibleType(integer, string, "1:0-1:32".loc))

        exec("MyData { name: \"Luna\" };")
        assertTypeError(UninitializedField("age", myDataType, "1:0-1:22".loc))

        exec("MyData { name: \"Luna\", age: 2, breed: \"Aussie\" };")
        assertTypeError(UndeclaredField("breed", myDataType, "1:0-1:47".loc))

        exec("data WrongData { oops: DoesNotExist }")
        assertTypeError(UndeclaredType("DoesNotExist", "1:0-1:36".loc))

        exec("data RightData { my: MyData }")
        assertEquals(listOf(), typeErrors)

        exec("var r = RightData { my: MyData { name: \"Luna\", age: 2 } };")
        assertEquals(listOf(), typeErrors)

        assertType("r", DataType("RightData", mapOf("my" to myDataType)))
        assertType("r.my", myDataType)
        assertType("r.my.age", integer)

        assertType("r.my.breed", nothing)
        assertTypeError(UndeclaredField("breed", myDataType, "1:4-1:5".loc))

        exec("var r = RightData { my: MyData { name: \"Luna\", age: 2, breed: \"Aussie\" } };")
        assertTypeError(UndeclaredField("breed", myDataType, "1:24-1:71".loc))

        exec("func intro(right: RightData): String { right.my.name + \", age \" + right.my.age }")
        exec("func introDeep(my: MyData): String { my.name + \", age \" + my.age }")
        assertType("r.intro()", string)
        assertType("r.my.introDeep()", string)
        assertEquals(listOf(), typeErrors)
    }

    @Test
    fun `interface`() {
        exec(
            """
            interface Animal {
              speak(): String
            }

            data Cat {}
            func speak(c: Cat): String { "meow" }

            data Dog {}
            func speak(d: Dog): String { "woof" }
            
            data Robot {}

            func makeSpeak(a: Animal): Animal {
              printLine(a.speak());
              a
            }
            """
        )
        val animalType = InterfaceType(
            "Animal",
            setOf(OverloadedFunction("speak", listOf(InterfaceFunction("speak", listOf(), string))))
        )
        assertType("makeSpeak(Cat {})", animalType)
        assertType("makeSpeak(Dog {})", animalType)
        assertEquals(listOf(), typeErrors)

        val robotType = DataType("Robot", emptyMap())
        assertType("makeSpeak(Robot {})", animalType)
        assertTypeError(IncompatibleType(animalType, robotType, "1:9-1:18".loc))
    }

    private fun assertType(code: String, expected: Type) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val expression = ParseTreeToAst().visitExpression(parser.expression())
        assertEquals(expected, expression.type())
    }

    private fun exec(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val sourceFile = ParseTreeToAst().visitSourceFile(parser.sourceFile())
        sourceFile.typeCheck()
    }

    private fun assertTypeError(expected: TypeError) {
        assertEquals(listOf(expected), typeErrors)
        typeErrors.clear()
    }
}