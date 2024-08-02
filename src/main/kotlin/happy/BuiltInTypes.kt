package happy

val nothing = BuiltInType("Nothing")
val any = BuiltInType("Any")
val integer = BuiltInType("Integer")
val string = BuiltInType("String")
val boolean = BuiltInType("Boolean")

val builtInTypes = setOf<Type>(any, integer, string, boolean)