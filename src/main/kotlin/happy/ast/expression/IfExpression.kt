package happy.ast.expression

import happy.Loc
import happy.Type
import happy.union

data class IfExpression(
    val condition: Expression,
    val ifTrue: ExpressionBlock,
    val ifFalse: Expression,
    override val loc: Loc
) : Expression {

    override fun type(): Type {
        val conditionMet = condition.type()
        // condition met must be boolean
        return union(setOf(ifTrue.type(), ifFalse.type()))
    }

    override fun eval(): Any {
        val conditionMet = condition.eval() as Boolean
        return if (conditionMet) ifTrue.eval()
        else ifFalse.eval()
    }
}
