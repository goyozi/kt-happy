@file:Suppress("unused")

package io.github.goyozi.kthappy

val builtIns = mutableListOf<BuiltInFunction>()

class BuiltInFunction(
    override val name: String,
    override val arguments: List<DeclaredArgument>,
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
    arguments = listOf(DeclaredArgument(any, "text")),
    returnType = nothing,
    implementation = { println(it.get("text")) }
)

val readLine = BuiltInFunction(
    name = "readLine",
    arguments = listOf(),
    returnType = string,
    implementation = { readln() }
)