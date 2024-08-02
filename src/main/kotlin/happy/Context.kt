package happy

import happy.ast.SourceFile

val sourceFiles = mutableMapOf<String, SourceFile>()
val scope = Scope<Any>()
val typingScope = Scope<Type>()
val typeErrors = mutableListOf<TypeError>()

fun resetContext() {
    sourceFiles.clear()
    scope.stack.clear()
    scope.stack.add(Layer())
    typingScope.stack.clear()
    typingScope.stack.add(Layer())
    typeErrors.clear()
}