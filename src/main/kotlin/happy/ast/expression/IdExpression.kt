package happy.ast.expression

import happy.*

data class IdExpression(val id: String, override val loc: Loc) : Expression {

    override fun type() = try {
        typingScope.get(id)
    } catch (_: IllegalStateException) {
        typeErrors.add(UnknownIdentifier(id, loc))
        nothing
    }

    override fun eval() = scope.get(id)
}
