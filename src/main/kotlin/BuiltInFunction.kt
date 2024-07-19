@file:Suppress("unused")

package io.github.goyozi.kthappy

val builtIns = mutableMapOf<String, BuiltInFunction>()

class BuiltInFunction(
    val name: String,
    val arguments: List<Pair<String, Type>>,
    val returnType: Type,
    val description: String,
    val implementation: (Scope<Any>) -> Any
) {

    init {
        builtIns[name] = this
    }
}

val printLine = BuiltInFunction(
    name = "printLine",
    arguments = listOf("text" to any),
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