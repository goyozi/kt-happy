package io.github.goyozi.kthappy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.FileInputStream

fun main(args: Array<String>) {
    val source = FileInputStream(args[0])
    val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromStream(source))))
    val visitor = Interpreter()
    visitor.visitSourceFile(parser.sourceFile())
}