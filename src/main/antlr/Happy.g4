grammar Happy;

sourceFile: (function | topLevelAction)* EOF;

topLevelAction: statement | expressionStatement;

function: 'function' ID '(' ID (',' ID)+ ')' '{' statement* expression '}';

statement: 'let' ID '=' expression ';' | whileLoop;

whileLoop: 'while' expression expressionBlock;

expressionStatement: expression ';';

expression
    : NUMBER
    | ID
    | STRING_LITERAL
    | expression PLUS expression
    | expression GT expression
    | expression EQ expression
    | expression NOT_EQ expression
    | call
    | ifExpression
    ;

expressionBlock: '{' (statement | expressionStatement)* expression '}';

ifExpression: 'if' expression expressionBlock 'else' expressionBlock;

call: ID '(' expression? (',' expression)* ')';

ID: [a-zA-Z]+;
NUMBER: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';
PLUS: '+';
GT: '>';
EQ: '==';
NOT_EQ: '!=';
WS: [ \t\r\n] -> skip;