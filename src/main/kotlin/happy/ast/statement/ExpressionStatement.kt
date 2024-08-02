package happy.ast.statement

import happy.Loc
import happy.ast.expression.Expression

data class ExpressionStatement(val expression: Expression, override val loc: Loc) : Statement {

    override fun typeCheck() {
        expression.type()
    }

    override fun eval() {
        expression.eval()
    }
}