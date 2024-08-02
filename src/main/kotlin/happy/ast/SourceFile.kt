package happy.ast

import happy.*
import happy.ast.statement.Statement

data class SourceFile(
    val imports: List<ImportDeclaration>,
    val types: List<TypeDeclaration>,
    val functions: List<FunctionDeclaration>,
    val statements: List<Statement>,
    override val loc: Loc
) : AstNode {

    fun typeCheck() {
        builtInTypes.forEach { typingScope.define(it.name, it) }
        builtIns.forEach { defineFunctionType(it.name, it) }

        imports.forEach(ImportDeclaration::typeCheck)
        types.forEach(TypeDeclaration::declareType)
        functions.forEach(FunctionDeclaration::declareFunction)
        statements.forEach(Statement::typeCheck)

        functions.forEach(FunctionDeclaration::typeCheckBody)
    }

    override fun eval() {
        builtIns.forEach { defineFunction(it.name, it) }

        imports.forEach(AstNode::eval)
        types.forEach(AstNode::eval)
        functions.forEach(AstNode::eval)
        statements.forEach(AstNode::eval)
    }
}