package io.github.goyozi.kthappy

interface Function {
    val name: String
    val arguments: List<DeclaredArgument>
    val returnType: Type

    fun invoke(interpreter: Interpreter): Any
}

data class OverloadedFunction(override val name: String, val functions: List<Function>) : Type {

    fun invoke(arguments: List<ArgumentValue>, interpreter: Interpreter): Any {
        val impl = getVariant(arguments.map { it.type }, interpreter.scope)
        interpreter.scope.enter(interpreter.functionParent[impl] ?: Layer())
        impl.arguments.forEachIndexed { i, at -> interpreter.scope.define(at.name, arguments[i].value) }
        val result = impl.invoke(interpreter)
        interpreter.scope.leave()
        return result
    }

    fun getVariant(argTypes: List<Type>, scope: Scope<*>) =
        functions.singleOrNull {
            it.arguments.size == argTypes.size
                    // todo: test with argument being enum type
                    && argTypes.mapIndexed { i, t -> it.arguments[i].type.assignableFrom(t, scope) }.all { it }
        } ?: throw Error("Type checking missed function type check. Function: $name Args: $argTypes Actual: $functions")
}

data class PreAppliedFunction(override val name: String, val firstArgument: ArgumentValue): Type

data class DeclaredArgument(val type: Type, val name: String)
data class ArgumentValue(val type: Type, val value: Any)

class CustomFunction(
    override val arguments: List<DeclaredArgument>,
    override val returnType: Type,
    private val ctx: HappyParser.FunctionContext
) : Function {
    override val name: String = ctx.sig.name.text

    override fun invoke(interpreter: Interpreter): Any {
        ctx.action().forEach(interpreter::visitAction)
        return interpreter.visitExpression(ctx.expression())
    }
}
