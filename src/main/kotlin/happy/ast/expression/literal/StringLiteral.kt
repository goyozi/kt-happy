package happy.ast.expression.literal

import happy.Loc
import happy.ast.expression.Expression
import happy.string

data class StringLiteral(val value: String, override val loc: Loc) : Expression {

    override fun type() = string

    override fun eval() = value
}