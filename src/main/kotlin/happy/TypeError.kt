package happy

import org.antlr.v4.runtime.ParserRuleContext

sealed class TypeError

data class IncompatibleType(val expectedType: Type, val actualType: Type, val loc: Loc) : TypeError() {
    override fun toString(): String {
        return "Incompatible types at $loc, expected: $expectedType, actual: $actualType"
    }
}

data class UnknownIdentifier(val name: String, val loc: Loc) : TypeError() {
    override fun toString(): String {
        return "Unknown identifier $name at $loc"
    }
}

data class UndeclaredType(val name: String, val loc: Loc) : TypeError() {
    override fun toString(): String {
        return "Undeclared type $name at $loc"
    }
}

data class UndeclaredField(val field: String, val type: Type, val loc: Loc) : TypeError() {
    override fun toString(): String {
        return "Undeclared field $field of type $type at $loc"
    }
}

data class UninitializedField(val field: String, val type: Type, val loc: Loc) : TypeError() {
    override fun toString(): String {
        return "Uninitialized field $field of type $type at $loc"
    }
}

data class Loc(val startLine: Int, val startPos: Int, val endLine: Int, val endPos: Int) {
    override fun toString(): String {
        return "$startLine:$startPos-$endLine:$endPos"
    }
}

val ParserRuleContext.loc get() = Loc(start.line, start.charPositionInLine, stop.line, stop.charPositionInLine)
