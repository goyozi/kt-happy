import io.github.goyozi.kthappy.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeCheckerTest {
    val typeChecker = TypeChecker()

    @Test
    fun intLiteral() {
        assertType("123", "Integer")
    }

    @Test
    fun stringLiteral() {
        assertType("\"it works\"", "String")
    }

    @Test
    fun booleanLiteral() {
        assertType("true", "Boolean")
        assertType("false", "Boolean")
    }

    @Test
    fun letStatement() {
        assertType("{ let x = 5 x }", "Integer")
        assertEquals(listOf(), typeChecker.typeErrors)
        assertType("{ let x: String = 5 x }", "String")
        assertTypeError(IncompatibleType("String", "Integer", "1:2-1:18".loc))
        exec("let x: UndeclaredType")
        assertType("x", "UndeclaredType")
        assertTypeError(UndeclaredType("UndeclaredType", "1:0-1:7".loc))
    }

    @Test
    fun assignmentStatement() {
        assertType("{ let x = 5 x = 10 x }", "Integer")
        assertEquals(listOf(), typeChecker.typeErrors)
        assertType("{ let x = 5 x = \"text\" x }", "Integer")
        assertTypeError(IncompatibleType("Integer", "String", "1:12-1:16".loc))
    }

    @Test
    fun functionCall() {
        exec(
            """
            function add(a: Integer, b: Integer): Integer {
              a + b
            }
            """
        )
        assertEquals(listOf(), typeChecker.typeErrors)
        assertType("add(5, 10)", "Integer")
    }

    @Test
    fun functionArgumentTypeCheck() {
        exec(
            """
            function add(a: Integer, b: Integer): Integer {
              a + b
            }
            """
        )
        assertType("add(5, \"not a number\")", "Integer")
        assertTypeError(IncompatibleType("Integer", "String", "1:3-1:21".loc))
    }

    @Test
    fun functionReturnTypeCheck() {
        exec(
            """
            function add(a: Integer, b: Integer): Integer {
              "not a number"
            }
            """
        )
        assertTypeError(IncompatibleType("Integer", "String", "2:12-4:12".loc))
    }

    @Test
    fun enumTypeCheck() {
        exec(
            """
            enum Choice { 'A, Integer }
            let choice: Choice
            """
        )
        assertType("choice", "Choice")
        exec("choice = 'A")
        assertEquals(listOf(), typeChecker.typeErrors)
        exec("choice = 5")
        assertEquals(listOf(), typeChecker.typeErrors)
        exec("choice = 'B")
        assertTypeError(IncompatibleType("Choice", "'B", "1:0-1:9".loc))
    }

    @Test
    fun ifExpression() {
        assertType("if 2 > 1 { 2 } else { 1 }", "Integer")
        assertType("if 2 > 1 { 2 } else { \"text!\" }", "Integer|String")
        assertType("if 2 > 1 { 2 } else if 3 > 2 { true } else { \"text!\" }", "Integer|Boolean|String")
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
            function correct(a: Integer): Bunch {
              if a > 3 { 4 }
              else { 'One }
            }
            """
        )
        assertEquals(listOf(), typeChecker.typeErrors)
        exec(
            """
            function wrong(a: Integer): Bunch {
              if a > 3 { 4 }
              else if a > 0 { 'One }
              else { "negative" }
            }
            """
        )
        assertEquals(
            listOf<TypeError>(IncompatibleType("Bunch", "Integer|'One|String", "2:12-6:12".loc)),
            typeChecker.typeErrors
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
            function returnIfSmall(num: Integer): Option<Integer> {
                if num <= 5 { num } else { 'None }
            }
            """
        )
        assertEquals(listOf(), typeChecker.typeErrors)
    }

    @Test
    fun genericEnumTypeCheck() {
        exec(
            """
            enum Opt<T> { T, 'None }
            let maybe: Opt<Integer>
            """
        )
        assertType("maybe", "Opt<Integer>")
        exec("maybe = 5")
        assertEquals(listOf(), typeChecker.typeErrors)
        exec("maybe = 'None")
        assertEquals(listOf(), typeChecker.typeErrors)
        exec("maybe = false")
        assertTypeError(IncompatibleType("Opt<Integer>", "Boolean", "1:0-1:8".loc))
    }

    @Test
    fun matchExpression() {
        assertType("match 3 { 3: \"three\", 5: \"five\", else: \"dunno\" }", "String")
        assertType("match 5 { 3: \"three\", 5: 5, else: \"dunno\" }", "String|Integer")
        assertType("match 7 { 3: \"three\", 5: 5, else: 'None }", "String|Integer|'None")
    }

    @Test
    fun dataType() {
        exec("data MyData { name: String, age: Integer }")
        assertType("MyData { name: \"Luna\", age: 2 }", "MyData")
        exec("let m = MyData { name: \"Luna\", age: 2 }")
        assertType("m", "MyData")
        assertType("m.name", "String")
        assertType("m.age", "Integer")

        assertType("m.breed", "Unknown")
        assertTypeError(UndeclaredField("breed", "MyData", "1:1-1:2".loc))

        exec("NotMyData { name: \"Luna\", age: \"2\" }")
        assertTypeError(UndeclaredType("NotMyData", "1:0-1:35".loc))

        exec("MyData { name: \"Luna\", age: \"2\" }")
        assertTypeError(IncompatibleType("Integer", "String", "1:0-1:32".loc))

        exec("MyData { name: \"Luna\" }")
        assertTypeError(UninitializedField("age", "MyData", "1:0-1:22".loc))

        exec("MyData { name: \"Luna\", age: 2, breed: \"Aussie\" }")
        assertTypeError(UndeclaredField("breed", "MyData", "1:0-1:47".loc))

        exec("data WrongData { oops: DoesNotExist }")
        assertTypeError(UndeclaredType("DoesNotExist", "1:0-1:36".loc))

        exec("data RightData { my: MyData }")
        assertEquals(listOf(), typeChecker.typeErrors)

        exec("let r = RightData { my: MyData { name: \"Luna\", age: 2 } }")
        assertEquals(listOf(), typeChecker.typeErrors)

        assertType("r", "RightData")
        assertType("r.my", "MyData")
        assertType("r.my.age", "Integer")

        assertType("r.my.breed", "Unknown")
        assertTypeError(UndeclaredField("breed", "MyData", "1:4-1:5".loc))

        exec("let r = RightData { my: MyData { name: \"Luna\", age: 2, breed: \"Aussie\" } }")
        assertTypeError(UndeclaredField("breed", "MyData", "1:24-1:71".loc))

        exec("function intro(right: RightData): String { right.my.name + \", age \" + right.my.age }")
        exec("function introDeep(my: MyData): String { my.name + \", age \" + my.age }")
        assertType("r.intro()", "String")
        assertType("r.my.introDeep()", "String")
        assertEquals(listOf(), typeChecker.typeErrors)
    }

    private fun assertType(code: String, expected: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val result = typeChecker.visitExpression(parser.expression())
        assertEquals(expected, result)
    }

    private fun exec(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        typeChecker.visitSourceFile(parser.sourceFile())
    }

    private fun assertTypeError(expected: TypeError) {
        assertEquals(listOf(expected), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()
    }

    private val String.loc
        get(): Loc {
            val startEnd = this.split("-")
            val startLinePos = startEnd[0].split(":")
            val endLinePos = startEnd[1].split(":")
            return Loc(startLinePos[0].toInt(), startLinePos[1].toInt(), endLinePos[0].toInt(), endLinePos[1].toInt())
        }
}