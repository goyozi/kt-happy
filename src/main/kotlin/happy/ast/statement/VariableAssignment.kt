package happy.ast.statement

import happy.Loc
import happy.ast.expression.Expression
import happy.checkType
import happy.scope
import happy.typingScope

data class VariableAssignment(val name: String, val value: Expression, override val loc: Loc) : Statement {

    override fun typeCheck() {
        val declaredType = typingScope.get(name)
        val expressionType = value.type()
        checkType(declaredType, expressionType, this)
    }

    override fun eval() {
        scope.assign(name, value.eval())
    }
}