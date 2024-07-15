import io.github.goyozi.kthappy.TypeChecker
import io.github.goyozi.kthappy.TypeError
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
        assertEquals(listOf(TypeError("1", "String", "Integer")), typeChecker.typeErrors)
    }

    @Test
    fun assignmentStatement() {
        assertType("{ let x = 5 x = 10 x }", "Integer")
        assertEquals(listOf(), typeChecker.typeErrors)
        assertType("{ let x = 5 x = \"text\" x }", "Integer")
        assertEquals(listOf(TypeError("1", "Integer", "String")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("1", "Integer", "String")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("2", "Integer", "String")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("1", "Choice", "'B")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("2", "Bunch", "Integer|'One|String")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("1", "Opt<Integer>", "Boolean")), typeChecker.typeErrors)
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
        assertEquals(listOf(TypeError("1", "MyData.", "breed")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("NotMyData { name: \"Luna\", age: \"2\" }")
        assertEquals(listOf(TypeError("1", "Existing", "Missing")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("MyData { name: \"Luna\", age: \"2\" }")
        assertEquals(listOf(TypeError("1", "Integer", "String")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("MyData { name: \"Luna\" }")
        assertEquals(listOf(TypeError("1", "MyData.age", "Missing")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("MyData { name: \"Luna\", age: 2, breed: \"Aussie\" }")
        assertEquals(listOf(TypeError("1", "MyData.", "breed")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("data WrongData { oops: DoesNotExist }")
        assertEquals(listOf(TypeError("1", "Existing", "DoesNotExist")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("data RightData { my: MyData }")
        assertEquals(listOf(), typeChecker.typeErrors)

        exec("let r = RightData { my: MyData { name: \"Luna\", age: 2 } }")
        assertEquals(listOf(), typeChecker.typeErrors)

        assertType("r", "RightData")
        assertType("r.my", "MyData")
        assertType("r.my.age", "Integer")

        assertType("r.my.breed", "Unknown")
        assertEquals(listOf(TypeError("1", "MyData.", "breed")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

        exec("let r = RightData { my: MyData { name: \"Luna\", age: 2, breed: \"Aussie\" } }")
        assertEquals(listOf(TypeError("1", "MyData.", "breed")), typeChecker.typeErrors)
        typeChecker.typeErrors.clear()

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
}