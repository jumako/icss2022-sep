grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';

//--- PARSER: ---
stylesheet: (variableAssignment | stylerule)* EOF;

variableAssignment : variableName ASSIGNMENT_OPERATOR expression SEMICOLON;
variableName: CAPITAL_IDENT;
stylerule: selector OPEN_BRACE declaration* CLOSE_BRACE;
ifClause: IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE OPEN_BRACE (declaration | ifClause)* CLOSE_BRACE (ELSE OPEN_BRACE (declaration | ifClause)* CLOSE_BRACE)?;
selector:
    LOWER_IDENT #tagSelector |
    CLASS_IDENT #classSelector |
    ID_IDENT #idSelector;

declaration: property COLON expression SEMICOLON;
property: LOWER_IDENT;
expression:
    expression PLUS term #addOperation|
    expression MIN term #subOperation|
    term # toTerm;
term:
    term MUL factor #mulOperation|
    factor #toFactor;
factor:
    PIXELSIZE #pixelLiteral |
    SCALAR #scalarLiteral|
    COLOR #colorLiteral|
    TRUE #trueLiteral|
    FALSE #falseLiteral|
    variableName #varRef
    ;


