grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

// possible ways to extend the grammar:
// 1. add constructors to the classes
// 2. allow other types of arrays and varargs



EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
NOT : '!' ;
AND : '&&' ;
LESS : '<' ;
PERIOD : '.' ;
COMMA : ',' ;

CLASS : 'class' ;

// primitive types
INT : 'int' ;

// ============ Added this ============
STRING : 'String' ;
CHAR : 'char' ;
VOID : 'void' ;
IMPORT : 'import' ;
STATIC : 'static' ;
ELLIPSIS : '...' ;
MAIN : 'main' ;
BOOLEAN : 'boolean' ;
NEW : 'new' ;
THIS : 'this' ;
TRUE : 'true' ;
FALSE : 'false' ;
ELSE : 'else' ;
IF : 'if' ;
WHILE : 'while' ;
EXTENDS : 'extends' ;

// ====================================

PUBLIC : 'public' ;
RETURN : 'return' ;


INTEGER : '0' | [1-9] [0-9]* ;
ID : [a-zA-Z_$] [a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT names+=(ID|MAIN) (PERIOD names+=(ID|MAIN))* SEMI;

classDecl
    : CLASS name=(ID|MAIN) (EXTENDS superclass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=(ID | MAIN) SEMI
    ;

type locals[boolean isArray=false, boolean isVarArgs=false]
    : name=INT LBRACK RBRACK {$isArray=true;}
    | name=INT ELLIPSIS {$isVarArgs=true; $isArray=true;}
    | name=INT
    | name=BOOLEAN
    | name=STRING
    | name=(ID | MAIN)
    ;

returnStmt
    : RETURN expr SEMI
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
        (STATIC {$isStatic=true;})? // added this but its optional from what i understood
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY
            varDecl*
            stmt*
            returnStmt
        RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC {$isStatic=true;}
        mainReturnType name=MAIN
        LPAREN STRING LBRACK RBRACK paramName=ID RPAREN
        LCURLY
            varDecl*
            stmt*
        RCURLY
    ;

mainReturnType locals[boolean isArray=false, boolean isVarArgs=false]
    : name= VOID;

param
    : type name=(ID | MAIN)
    ;

stmt
    : LCURLY stmt* RCURLY #BlockStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | name=(ID | MAIN) EQUALS expr SEMI #AssignStmt
    | name=(ID | MAIN) LBRACK expr RBRACK EQUALS expr SEMI #ArrayAssignStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | value=INTEGER #IntegerLiteral
    | name=(TRUE |FALSE) #BoolLiteral
    | name=(ID|MAIN) #VarRefExpr
    | THIS #ThisLiteral
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayCreationExpr
    | expr LBRACK expr RBRACK #ArrayAccessExpr
    | expr PERIOD name=(ID | MAIN) LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr
    | NEW name=INT LBRACK expr RBRACK #NewArrayExpr
    | NEW name=ID LPAREN RPAREN #NewExpr
    | expr PERIOD name=ID #ArrayLengthExpr
    | expr op = (MUL | DIV) expr #BinaryExpr
    | expr op = (ADD | SUB) expr #BinaryExpr
    | op=NOT expr #UnaryExpr
    | expr op =LESS expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    ;