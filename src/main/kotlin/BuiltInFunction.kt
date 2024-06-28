@file:Suppress("unused")

package io.github.goyozi.kthappy

val builtIns = mutableMapOf<String, BuiltInFunction>()

class BuiltInFunction(
    val name: String,
    val arguments: List<Pair<String, String>>,
    val description: String,
    val implementation: (Scope) -> Value
) {

    init {
        builtIns[name] = this
    }
}

val printLine = BuiltInFunction(
    name = "printLine",
    arguments = listOf("text" to "Text to print"),
    description = "Prints provided text and a newline at the end",
    implementation = { println(it.get("text").value); none }
)

val readLine = BuiltInFunction(
    name = "readLine",
    arguments = listOf(),
    description = "Reads a line from standard input and returns the text without newline at the end",
    implementation = { Value("String", readln()) }
)