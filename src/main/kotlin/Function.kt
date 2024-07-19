package io.github.goyozi.kthappy

import HappyParser

data class Function(val name: String, val variants: Map<List<Type>, HappyParser.FunctionContext>)

data class PreAppliedFunction(
    val name: String,
    val firstArgument: Any,
    val firstArgumentType: Type,
    val variants: Map<List<Type>, HappyParser.FunctionContext>
)

