package happy.ast.expression

import happy.Loc
import happy.Type
import happy.ast.statement.Statement
import happy.scope
import happy.typingScope

data class ExpressionBlock(
    val statements: List<Statement>,
    val returnExpression: Expression,
    override val loc: Loc
) : Expression {

    override fun type(): Type {
        typingScope.enter()
        statements.forEach(Statement::typeCheck)
        return returnExpression.type().also { typingScope.leave() }
    }

    override fun eval(): Any {
        scope.enter()
        statements.forEach { it.eval() }
        return returnExpression.eval().also { scope.leave() }
    }
}