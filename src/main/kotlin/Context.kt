package io.github.goyozi.kthappy

import HappyParser
import org.antlr.v4.runtime.tree.ParseTreeProperty

// todo: make it non global or clear it before tests
val functions = ParseTreeProperty<Function>()
val expressionTypes = ParseTreeProperty<Type>()
val sourceFileTrees = mutableMapOf<String, HappyParser.SourceFileContext>()
