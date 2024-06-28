grammar Happy;

sourceFile: (function | statement | expression)* expression EOF;

function: 'function' ID '(' ID (',' ID)+ ')' '{' statement* expression '}';

statement: 'let' ID '=' expression ';';

expression
    : NUMBER
    | ID
    | expression PLUS expression
    | call
    ;

call: ID '(' expression (',' expression)+ ')';

ID: [a-zA-Z]+;
NUMBER: ('0' .. '9')+;
PLUS: '+';
WS: [ \r\n] -> skip;