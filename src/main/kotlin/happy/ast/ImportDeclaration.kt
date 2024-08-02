package happy.ast

import happy.*

data class ImportDeclaration(val filePath: String, val ids: List<String>, override val loc: Loc) : AstNode {

    fun typeCheck() {
        val currentScope = typingScope.stack.last()
        typingScope.enter(Layer())
        // todo: shouldn't happen multiple times for the same file
        sourceFiles[filePath]!!.typeCheck()
        for (id in ids) {
            currentScope.bindings[id] = typingScope.get(id)
        }
        typingScope.leave()
    }

    override fun eval() {
        val currentScope = scope.stack.last()
        scope.enter(Layer())
        // todo: shouldn't happen multiple times for the same file
        sourceFiles[filePath]!!.eval()
        for (id in ids) {
            currentScope.bindings[id] = scope.get(id)
        }
        scope.leave()
    }
}