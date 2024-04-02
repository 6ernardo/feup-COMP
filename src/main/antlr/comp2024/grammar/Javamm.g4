grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}



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
LENGTH : 'length' ;
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

importName : name=ID;

importDecl
    : IMPORT (importName PERIOD)* importName SEMI;

classDecl
    : CLASS name=ID (EXTENDS superclass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI #VariableDecl
    | type name=MAIN SEMI #VariableDecl
    ;

type locals[boolean isArray=false, boolean isVarArgs=false]
    : name= INT LBRACK RBRACK {$isArray=true;}
    | name= INT ELLIPSIS {$isVarArgs=true;}
    | name= INT
    | name= BOOLEAN
    | name= STRING
    | name= ID
    ;

returnStmt
    : RETURN expr SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY
            varDecl*
            stmt*
            returnStmt
        RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC mainReturnType name=MAIN
        LPAREN STRING LBRACK RBRACK paramName=ID RPAREN
        LCURLY
            varDecl*
            stmt*
        RCURLY
    ;

mainReturnType locals[boolean isArray=false, boolean isVarArgs=false]
    : name= VOID;

param
    : type name=ID  #Parameter
    | type name=MAIN #Parameter
    ;

stmt
    : LCURLY stmt* RCURLY #BlockStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | name=ID EQUALS expr SEMI #AssignStmt
    | name=ID LBRACK expr RBRACK EQUALS expr SEMI #ArrayAssignStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | op=NOT expr #UnaryExpr
    | expr op = (MUL | DIV) expr #BinaryExpr
    | expr op = (ADD | SUB) expr #BinaryExpr
    | expr op =LESS expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | expr LBRACK expr RBRACK #ArrayAccessExpr
    | expr PERIOD LENGTH #ArrayLengthExpr
    | expr PERIOD name=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr
    | NEW name=INT LBRACK expr RBRACK #NewExpr
    | NEW name=ID LPAREN RPAREN #NewExpr
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayCreationExpr
    | value=INTEGER #IntegerLiteral
    | TRUE #TrueLiteral
    | FALSE #FalseLiteral
    | name=ID #VarRefExpr
    | THIS #ThisLiteral
    ;