package happy.ast.statement

import happy.Loc
import happy.ast.expression.Expression
import happy.scope

data class WhileLoop(val condition: Expression, val statements: List<Statement>, override val loc: Loc) : Statement {

    override fun typeCheck() {
        scope.enter()
        // todo: check it's a boolean
        condition.type()
        statements.forEach(Statement::typeCheck)
        scope.leave()
    }

    override fun eval() {
        scope.enter()
        while (condition.eval() == true) statements.forEach(Statement::eval)
        scope.leave()
    }
}