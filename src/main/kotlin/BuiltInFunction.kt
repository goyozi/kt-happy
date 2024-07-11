@file:Suppress("unused")

package io.github.goyozi.kthappy

val builtIns = mutableMapOf<String, BuiltInFunction>()

class BuiltInFunction(
    val name: String,
    val arguments: List<Pair<String, String>>,
    val returnType: String,
    val description: String,
    val implementation: (Scope<Any>) -> Any
) {

    init {
        builtIns[name] = this
    }
}

val printLine = BuiltInFunction(
    name = "printLine",
    arguments = listOf("text" to "Any"),
    returnType = "None",
    description = "Prints provided text and a newline at the end",
    implementation = { println(it.get("text")) }
)

val readLine = BuiltInFunction(
    name = "readLine",
    arguments = listOf(),
    returnType = "String",
    description = "Reads a line from standard input and returns the text without newline at the end",
    implementation = { readln() }
)