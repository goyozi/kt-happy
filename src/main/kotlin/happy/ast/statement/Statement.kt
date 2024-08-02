package happy.ast.statement

import happy.ast.AstNode

interface Statement : AstNode {
    fun typeCheck()
    override fun eval()
}