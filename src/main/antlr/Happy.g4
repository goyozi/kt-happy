grammar Happy;

sourceFile: importStatement* (COMMENT | typeDeclaration | function | statement)* EOF;

importStatement: 'import' (paths+=ID '.')* '{' (symbols+=ID ',')* (symbols+=ID)? '}';

typeDeclaration: data | enum | interface;

data: 'data' name=ID '{' (keyType ',')* (keyType)? '}';

enum: 'enum' name=ID ('<' genericType=ID '>')? '{' (values+=typeOrSymbol ',')* (values+=typeOrSymbol)? '}';

typeOrSymbol: typeSpec | symbol;

interface: 'interface' name=ID '{' (sigs+=functionSignature)* '}';

function: 'func' sig=functionSignature '{' statement* expression '}';

functionSignature: name=ID '(' (arguments+=keyType ',')* (arguments+=keyType)? ')' ':' returnType=typeSpec;

keyType: name=ID ':' type=typeSpec;

statement
    : variableDeclaration
    | variableAssignment
    | whileLoop
    | forLoop
    | expressionStatement;

variableDeclaration: 'var' ID (':' typeSpec)? ('=' expression)? ';';

variableAssignment: ID '=' expression ';';

whileLoop: 'while' expression '{' statement* '}';

forLoop: 'for' ID 'in' INTEGER_LITERAL '..' INTEGER_LITERAL '{' statement* '}';

expressionStatement: expression ';';

expression
    : expression postfixExpression #complexExpression
    | '!' expression #negation
    | '-' expression #unaryMinus
    | left=expression op=('*'|'/'|'%') right=expression #multiplicative
    | left=expression op=('+'|'-') right=expression #additive
    | left=expression op=('>'|'<'|'>='|'<='|'=='|'!=') right=expression #comparison
    | ifExpression #ifExpr
    | matchExpression #matchExpr
    | expressionBlock #blockExpr
    | ID '{' (keyExpression ',')* (keyExpression)? '}' #constructor
    | primaryExpression #simpleExpression;

primaryExpression
    : '(' expression ')' #expressionInBrackets
    | ('true'|'false') #booleanLiteral
    | INTEGER_LITERAL #integerLiteral
    | STRING_LITERAL #stringLiteral
    | ID #identifier
    | symbol #symbolLiteral
    ;

symbol: SYMBOL;

postfixExpression
    : '(' expression? (',' expression)* ')' #functionCall
    | '.' ID #dotCall
    | 'as' typeSpec #typeCast;

keyExpression: ID ':' expression;

ifExpression: 'if' condition=expression ifTrue=expressionBlock 'else' (ifFalse=expressionBlock | elseIf=ifExpression);

matchExpression: 'match' expression '{' patternValue* matchElse '}';

patternValue: pattern=expression ':' value=expression ',';

matchElse: 'else' ':' expression;

expressionBlock: '{' statement* expression '}';

typeSpec: type=ID ('<' genericType=ID '>')?;

COMMENT: '//' ~[\r\n]*;
SYMBOL: '\''ID;
ID: [a-zA-Z]+;
INTEGER_LITERAL: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';
WS: [ \t\r\n] -> skip;