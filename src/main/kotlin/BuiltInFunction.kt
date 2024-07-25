@file:Suppress("unused")

package io.github.goyozi.kthappy

val builtIns = mutableListOf<BuiltInFunction>()

class BuiltInFunction(
    val name: String,
    override val arguments: List<DeclaredArgument>,
    val returnType: Type,
    val description: String,
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
    description = "Prints provided text and a newline at the end",
    implementation = { println(it.get("text")) }
)

val readLine = BuiltInFunction(
    name = "readLine",
    arguments = listOf(),
    returnType = string,
    description = "Reads a line from standard input and returns the text without newline at the end",
    implementation = { readln() }
)