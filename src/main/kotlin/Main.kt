package io.github.goyozi.kthappy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.FileInputStream

fun main(args: Array<String>) {
    val source = FileInputStream(args[0])
    val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromStream(source))))

    val typeChecker = TypeChecker()
    typeChecker.visitSourceFile(parser.sourceFile())
    for (typeError in typeChecker.typeErrors) {
        System.err.println(typeError)
    }
    parser.reset()

    Interpreter().visitSourceFile(parser.sourceFile())
}