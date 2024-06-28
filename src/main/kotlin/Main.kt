package io.github.goyozi.kthappy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class App

fun main() {
    val source = App::class.java.classLoader.getResourceAsStream("example.happy")!!
    val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromStream(source))))
    val visitor = HappyVisitor()

    println(visitor.visitSourceFile(parser.sourceFile()))
}