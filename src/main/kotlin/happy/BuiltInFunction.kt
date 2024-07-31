@file:Suppress("unused")

package happy

val builtIns = mutableListOf<BuiltInFunction>()

class BuiltInFunction(
    override val name: String,
    override val arguments: List<Parameter>,
    override val returnType: Type,
    val implementation: (Scope<Any>) -> Any
) : Function {

    init {
        builtIns.add(this)
    }

    override fun invoke(interpreter: Interpreter): Any {
        return implementation(interpreter.scope)
    }
}

val printLine = BuiltInFunction(
    name = "printLine",
    arguments = listOf(Parameter("text", any)),
    returnType = nothing,
    implementation = { println(it.get("text")) }
)

val readLine = BuiltInFunction(
    name = "readLine",
    arguments = listOf(),
    returnType = string,
    implementation = { readln() }
)