import io.github.goyozi.kthappy.Interpreter
import io.github.goyozi.kthappy.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// change this to increase "difficulty", 30-35 work nicely
private const val n = 20

class BallparkPerformanceTest {

    lateinit var typeChecker: TypeChecker
    lateinit var interpreter: Interpreter

    @BeforeEach
    fun setUp() {
        typeChecker = TypeChecker()
        interpreter = Interpreter()

        exec(
            """
            function fib(n: Integer): Integer {
              if n < 2 { n }
              else { fib(n - 1) + fib(n - 2) }
            }
            """
        )
        val expression = parseExpression("fib(10)")
        assertExpression(expression, 55)
        exec("fib(20)")

        assertEquals(55, fib(10))
        fib(20)
    }

    @Test
    fun happy() {
        val start = System.currentTimeMillis()
        exec("fib($n)")
        val end = System.currentTimeMillis()
        println("happy time: ${end - start}")
    }

    @Test
    fun unhappy() {
        val start = System.currentTimeMillis()
        fib(n)
        val end = System.currentTimeMillis()
        println("unhappy time: ${end - start}")
    }

    fun fib(n: Int): Int = if (n < 2) n else fib(n - 1) + fib(n - 2)

    private fun parseExpression(code: String): HappyParser.ExpressionContext {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val expression = parser.expression()
        typeChecker.visitExpression(expression)
        return expression
    }

    private fun assertExpression(expression: HappyParser.ExpressionContext, expected: Any) {
        val result = interpreter.visitExpression(expression)
        assertEquals(expected, result)
    }

    private fun exec(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val sourceFile = parser.sourceFile()
        typeChecker.visitSourceFile(sourceFile)
        interpreter.visitSourceFile(sourceFile)
    }
}