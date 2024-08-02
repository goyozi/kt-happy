package happy.ast.expression.literal

import happy.Loc
import happy.ast.expression.Expression
import happy.boolean

data class BooleanLiteral(val value: Boolean, override val loc: Loc) : Expression {

    override fun type() = boolean

    override fun eval() = value
}