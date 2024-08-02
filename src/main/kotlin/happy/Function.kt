package happy

import happy.ast.expression.Expression
import happy.ast.statement.Statement

interface Function {
    val name: String
    val arguments: List<Parameter>
    val returnType: Type

    var parentScope: Layer<Any>

    fun invoke(arguments: Array<Any>): Any {
        scope.enter(parentScope)
        this.arguments.forEachIndexed { i, at -> scope.define(at.name, arguments[i]) }
        val result = invoke()
        scope.leave()
        return result
    }

    fun invoke(): Any
}

data class OverloadedFunction(override val name: String, val functions: List<Function>) : Type {

    fun getVariant(argTypes: List<Type>, scope: Scope<*>) =
        findVariant(argTypes, scope).singleOrNull()
            ?: throw Error("Type checking missed function type check. Function: $name Args: ${argTypes.map { it.name }} Actual: ${functions.map { it.arguments.map { it.type.name } }}")

    fun getStaticVariant(argTypes: List<Type>, scope: Scope<*>) =
        findVariant(argTypes, scope).singleOrNull { it !is InterfaceFunction }
            ?: throw Error("Type checking missed function type check. Function: $name Args: $argTypes Actual: $functions")

    private fun findVariant(argTypes: List<Type>, scope: Scope<*>) =
        functions.filter { matchingArguments(it.arguments, argTypes, scope) }

    private fun matchingArguments(expected: List<Parameter>, actual: List<Type>, scope: Scope<*>) =
        expected.size == actual.size
                // todo: test with argument being enum type
                && List(expected.size) { i -> expected[i].type.assignableFrom(actual[i], scope) }.all { it }
}

data class PreAppliedFunction(override val name: String, val firstArgument: Argument): Type

data class Parameter(val name: String, val type: Type)
data class Argument(val value: Any, val type: Type)

data class CustomFunction(
    override val name: String,
    override val arguments: List<Parameter>,
    override val returnType: Type,
    val statements: List<Statement>,
    val returnExpression: Expression
) : Function {
    override lateinit var parentScope: Layer<Any>

    override fun invoke(): Any {
        statements.forEach(Statement::eval)
        return returnExpression.eval()
    }
}

fun defineFunctionType(name: String, impl: Function) {
    try {
        val function = typingScope.get(name) as OverloadedFunction // todo: might blow up
        val newFunction = OverloadedFunction(name, function.functions + listOf(impl))
        typingScope.assign(name, newFunction)
    } catch (_: IllegalStateException) {
        typingScope.define(name, OverloadedFunction(name, listOf(impl)))
    }
}

fun defineFunction(name: String, impl: Function) {
    try {
        val function = scope.get(name) as OverloadedFunction // todo: might blow up
        val newFunction = OverloadedFunction(name, function.functions + listOf(impl))
        scope.assign(name, newFunction)
    } catch (_: IllegalStateException) {
        scope.define(name, OverloadedFunction(name, listOf(impl)))
    }
}
