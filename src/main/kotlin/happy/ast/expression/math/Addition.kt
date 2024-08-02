package happy.ast.expression.math

import happy.Loc
import happy.Type
import happy.ast.expression.Expression
import happy.string

data class Addition(val left: Expression, val right: Expression, override val loc: Loc) : Expression {
    private lateinit var resolvedType: Type

    override fun type() = left.type().also {
        resolvedType = it
        left.type()
        right.type()
    }

    override fun eval(): Any {
        return if (resolvedType == string) left.eval() as String + right.eval()
        else left.eval() as Int + right.eval() as Int
    }
}