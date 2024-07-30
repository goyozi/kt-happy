package io.github.goyozi.kthappy

import HappyParser
import org.antlr.v4.runtime.tree.ParseTreeProperty

// todo: make it non global or clear it before tests
val interfaceTypes = ParseTreeProperty<InterfaceType>()
val functions = ParseTreeProperty<Function>()
val argumentTypes = ParseTreeProperty<List<Type>>()
val expressionTypes = ParseTreeProperty<Type>()
val sourceFileTrees = mutableMapOf<String, HappyParser.SourceFileContext>()

val literals = ParseTreeProperty<Any>()
val identifiers = ParseTreeProperty<String>()
val binaryOps = ParseTreeProperty<(Any, Any) -> Any>()
val iioTypes = ParseTreeProperty<InterfaceType>()
val resolvedArgumentExpressions = ParseTreeProperty<List<HappyParser.ExpressionContext>>()
val resolvedFunctionCalls = ParseTreeProperty<Function>()
