package happy.ast.statement

import happy.Loc
import happy.integer
import happy.scope
import happy.typingScope

data class ForLoop(
    val iteratorName: String,
    val iterable: Iterable<*>,
    val statements: List<Statement>,
    override val loc: Loc
) : Statement {

    override fun typeCheck() {
        typingScope.enter()
        typingScope.define(iteratorName, integer)
        statements.forEach(Statement::typeCheck)
        typingScope.leave()
    }

    override fun eval() {
        scope.enter()
        for (i in iterable) {
            scope.define(iteratorName, i!!)
            statements.forEach(Statement::eval)
        }
        scope.leave()
    }
}