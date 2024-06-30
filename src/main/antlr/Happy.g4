grammar Happy;

sourceFile: (COMMENT | data | function | action)* EOF;

COMMENT: '//' ~[\r\n]*;

action: statement | expression;

data: 'data' ID '{' (keyValue ',')* (keyValue)? '}';

keyValue: ID ':' ID;

function: 'function' ID '(' ID (',' ID)+ ')' ':' ID '{' action* expression '}';

statement: 'let'? ID (':' ID)? '=' expression | whileLoop | forLoop;

whileLoop: 'while' expression '{' action* '}';

forLoop: 'for' ID 'in' NUMBER '..' NUMBER '{' action* '}';

expression
    : NUMBER
    | ID
    | STRING_LITERAL
    | expression TIMES expression
    | expression DIV expression
    | expression MOD expression
    | expression PLUS expression
    | expression MINUS expression
    | expression GT expression
    | expression LT expression
    | expression GT_EQ expression
    | expression LT_EQ expression
    | expression EQ expression
    | expression NOT_EQ expression
    | call
    | ifExpression
    | constructor
    | dotAccess
    | expressionBlock
    ;

expressionBlock: '{' (action)* expression '}';

ifExpression: 'if' expression expressionBlock 'else' (expressionBlock | ifExpression);

call: ID '(' expression? (',' expression)* ')';

constructor: ID '{' (keyExpression ',')* (keyExpression)? '}';

keyExpression: ID ':' expression;

dotAccess: ID '.' ID;

ID: [a-zA-Z]+;

NUMBER: [0-9]+;
STRING_LITERAL: '"' ~ ["\r\n]* '"';

PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
MOD: '%';

GT: '>';
LT: '<';
GT_EQ: '>=';
LT_EQ: '<=';
EQ: '==';
NOT_EQ: '!=';

WS: [ \t\r\n] -> skip;