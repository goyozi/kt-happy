grammar Happy;

sourceFile: importStatement* (COMMENT | data | enum | function | action)* EOF;

importStatement: 'import' (paths+=ID '.')* '{' (symbols+=ID ',')* (symbols+=ID)? '}';

data: 'data' name=ID '{' (keyType ',')* (keyType)? '}';

enum: 'enum' name=ID ('<' genericType=ID '>')? '{' (values+=typeOrSymbol ',')* (values+=typeOrSymbol)? '}';

typeOrSymbol: ID | SYMBOL;

function: 'function' name=ID '(' (arguments+=keyType ',')* (arguments+=keyType)? ')' ':' returnType=typeSpec '{' action* expression '}';

keyType: name=ID ':' type=typeSpec;

action: (statement | expression) ';'?;

statement
    : variableDeclaration
    | variableAssignment
    | whileLoop
    | forLoop;

variableDeclaration: 'let' ID (':' typeSpec)? ('=' expression)?;

variableAssignment: ID '=' expression;

whileLoop: 'while' expression '{' action* '}';

forLoop: 'for' ID 'in' INTEGER_LITERAL '..' INTEGER_LITERAL '{' action* '}';

expression
    : expression postfixExpression #complexExpression
    | '!' expression #negation
    | '-' expression #unaryMinus
    | expression '*' expression #multiplication
    | expression '/' expression #division
    | expression '%' expression #modulus
    | expression '+' expression #addition
    | expression '-' expression #subtraction
    | expression '>' expression #greaterThan
    | expression '<' expression #lessThan
    | expression '>=' expression #greaterOrEqual
    | expression '<=' expression #lessOrEqual
    | expression '==' expression #equalTo
    | expression '!=' expression #notEqual
    | ifExpression #ifExpr
    | matchExpression #matchExpr
    | expressionBlock #blockExpr
    | ID '{' (keyExpression ',')* (keyExpression)? '}' #constructor
    | primaryExpression #simpleExpression;

primaryExpression
    : '(' expression ')' #expressionInBrackets
    | 'true' #trueLiteral
    | 'false' #falseLiteral
    | INTEGER_LITERAL #integerLiteral
    | STRING_LITERAL #stringLiteral
    | ID #identifier
    | SYMBOL #symbol
    ;

postfixExpression
    : '(' expression? (',' expression)* ')' #functionCall
    | '.' ID #dotCall
    | 'as' typeSpec #typeCast;

keyExpression: ID ':' expression;

ifExpression: 'if' expression expressionBlock 'else' (expressionBlock | ifExpression);

matchExpression: 'match' expression '{' patternValue* matchElse '}';

patternValue: pattern=expression ':' value=expression ',';

matchElse: 'else' ':' expression;

expressionBlock: '{' (action)* expression '}';

typeSpec: type=ID ('<' genericType=ID '>')?;

COMMENT: '//' ~[\r\n]*;
SYMBOL: '\''ID;
ID: [a-zA-Z]+;
INTEGER_LITERAL: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';
WS: [ \t\r\n] -> skip;