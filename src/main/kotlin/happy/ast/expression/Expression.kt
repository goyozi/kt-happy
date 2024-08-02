package happy.ast.expression

import happy.Type
import happy.ast.AstNode

interface Expression : AstNode {
    fun type(): Type
}
