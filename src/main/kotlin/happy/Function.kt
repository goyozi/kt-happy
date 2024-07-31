package happy

interface Function {
    val name: String
    val arguments: List<Parameter>
    val returnType: Type

    fun invoke(arguments: Array<Any>, interpreter: Interpreter): Any {
        interpreter.scope.enter(interpreter.functionParent[this] ?: Layer())
        this.arguments.forEachIndexed { i, at -> interpreter.scope.define(at.name, arguments[i]) }
        val result = invoke(interpreter)
        interpreter.scope.leave()
        return result
    }

    fun invoke(interpreter: Interpreter): Any
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
    override val arguments: List<Parameter>,
    override val returnType: Type,
    private val ctx: HappyParser.FunctionContext
) : Function {
    override val name: String = ctx.sig.name.text

    override fun invoke(interpreter: Interpreter): Any {
        ctx.action().forEach(interpreter::visitAction)
        return interpreter.visitExpression(ctx.expression())
    }
}
