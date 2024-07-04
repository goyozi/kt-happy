grammar Happy;

sourceFile: (COMMENT | data | function | action)* EOF;

data: 'data' ID '{' (keyType ',')* (keyType)? '}';

function: 'function' name=ID '(' arguments+=keyType (',' arguments+=keyType)+ ')' ':' returnType=ID '{' action* expression '}';

keyType: name=ID ':' type=ID;

action: statement | expression;

statement
    : variableDeclaration
    | variableAssignment
    | whileLoop
    | forLoop;

variableDeclaration: 'let' ID (':' ID)? '=' expression;

variableAssignment: ID '=' expression;

whileLoop: 'while' expression '{' action* '}';

forLoop: 'for' ID 'in' INTEGER_LITERAL '..' INTEGER_LITERAL '{' action* '}';

expression
    : '(' expression ')' #expressionInBrackets
    | 'true' #trueLiteral
    | 'false' #falseLiteral
    | INTEGER_LITERAL #integerLiteral
    | STRING_LITERAL #stringLiteral
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
    | ID #identifier
    | ID '(' expression? (',' expression)* ')' #functionCall
    | ID '{' (keyExpression ',')* (keyExpression)? '}' #constructor
    | ID '.' ID #dotCall
    | ifExpression #ifExpr
    | expressionBlock #blockExpr
    ;

keyExpression: ID ':' expression;

ifExpression: 'if' expression expressionBlock 'else' (expressionBlock | ifExpression);

expressionBlock: '{' (action)* expression '}';


COMMENT: '//' ~[\r\n]*;
ID: [a-zA-Z]+;
INTEGER_LITERAL: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';
WS: [ \t\r\n] -> skip;