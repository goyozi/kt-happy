package happy.ast.expression

import happy.Loc
import happy.Type
import happy.union

data class MatchExpression(
    val value: Expression,
    val patternValues: List<Pair<Expression, Expression>>,
    val elseValue: Expression,
    override val loc: Loc
) : Expression {

    override fun type(): Type {
        value.type()
        val resultTypes = mutableSetOf<Type>()
        for (patternValue in patternValues) {
            patternValue.first.type()
            resultTypes.add(patternValue.second.type())
        }
        resultTypes.add(elseValue.type())
        return union(resultTypes)
    }

    override fun eval(): Any {
        val matchValue = value.eval()
        for (patternValue in patternValues) {
            if (matchValue == patternValue.first.eval()) {
                return patternValue.second.eval()
            }
        }
        return elseValue.eval()
    }
}