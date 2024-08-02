package happy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.FileInputStream

fun main(args: Array<String>) {
    val sourceFileTree = parseSourceFile(args[0])
    val sourceFile = ParseTreeToAst().visitSourceFile(sourceFileTree)

    sourceFile.typeCheck()
    for (typeError in typeErrors) {
        System.err.println(typeError)
    }

    sourceFile.eval()
}

fun parseSourceFile(path: String): HappyParser.SourceFileContext {
    val source = FileInputStream(path)
    val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromStream(source))))
    val sourceFileTree = parser.sourceFile()
    return sourceFileTree
}