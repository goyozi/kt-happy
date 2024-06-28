grammar Happy;

sourceFile: (function | statement | expressionStatement)* EOF;

function: 'function' ID '(' ID (',' ID)+ ')' '{' statement* expression '}';

statement: 'let' ID '=' expression ';';

expressionStatement: expression ';';

expression
    : NUMBER
    | ID
    | STRING_LITERAL
    | expression PLUS expression
    | call
    ;

call: ID '(' expression (',' expression)*')';

ID: [a-zA-Z]+;
NUMBER: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';
PLUS: '+';
WS: [ \t\r\n] -> skip;