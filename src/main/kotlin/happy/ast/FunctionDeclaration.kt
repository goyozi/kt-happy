package happy.ast

import happy.*
import happy.Function
import happy.ast.expression.Expression
import happy.ast.statement.Statement

data class FunctionDeclaration(
    val signature: FunctionSignature,
    val statements: List<Statement>,
    val returnExpression: Expression,
    override val loc: Loc
) : AstNode {
    private lateinit var function: Function

    fun declareFunction() {
        val arguments = signature.arguments.map { Parameter(it.key, it.value.toType()) }
        function =
            CustomFunction(signature.name, arguments, signature.returnType.toType(), statements, returnExpression)
        defineFunctionType(function.name, function)
    }

    fun typeCheckBody() {
        typingScope.enter()

        signature.arguments.forEach {
            typingScope.define(it.key, it.value.toType())
        }

        // todo: test that we evaluate the actions
        statements.forEach(Statement::typeCheck)

        val declaredReturnType = signature.returnType.toType()
        val actualReturnType = returnExpression.type()
        checkType(declaredReturnType, actualReturnType, this)

        typingScope.leave()
    }

    override fun eval() {
        function.parentScope = scope.stack.last()
        defineFunction(function.name, function)
    }
}