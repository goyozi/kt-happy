package happy.ast

import happy.Loc

interface AstNode {
    val loc: Loc

    fun eval(): Any
}
