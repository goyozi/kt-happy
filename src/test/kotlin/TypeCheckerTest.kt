import io.github.goyozi.kthappy.TypeChecker
import io.github.goyozi.kthappy.TypeError
import io.github.goyozi.kthappy.Value
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
        defineFunction(
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
    fun functionReturnTypeCheck() {
        defineFunction(
            """
            function add(a: Integer, b: Integer): Integer {
              "not a number"
            }
            """
        )
        assertEquals(listOf(TypeError("2", "Integer", "String")), typeChecker.typeErrors)
    }

    private fun assertType(code: String, expected: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val result = typeChecker.visitExpression(parser.expression())
        assertEquals(expected, result)
    }

    private fun defineFunction(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        typeChecker.visitFunction(parser.function())
    }
}