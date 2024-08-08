package happy.ast.expression.literal

import happy.Loc
import happy.ast.expression.Expression
import happy.integer

data class IntLiteral(val value: Int, override val loc: Loc) : Expression {

    override fun type() = integer

    override fun eval() = value

    override fun intEval() = value
}