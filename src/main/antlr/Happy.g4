grammar Happy;

sourceFile: (COMMENT | function | action)* EOF;

COMMENT: '//' ~[\r\n]*;

action: statement | expression;

function: 'function' ID '(' ID (',' ID)+ ')' '{' action* expression '}';

statement: 'let' ID '=' expression | whileLoop;

whileLoop: 'while' expression expressionBlock;

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

expressionBlock: '{' (action)* expression '}';

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