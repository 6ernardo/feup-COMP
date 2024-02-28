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



program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name=ID ( '.' ID)* SEMI;

classDecl
    : CLASS name=ID (EXTENDS superclass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type locals[boolean isArray=false]
    : name= INT LBRACK RBRACK {$isArray=true;}
    | name= INT ELLIPSIS {$isArray=true;}
    | name= INT
    | name= BOOLEAN
    | name= STRING
    | name= ID
    | name= STRING
    ;

mainParam locals[boolean isArray=false]
    : mainParamType LBRACK RBRACK name=ID {$isArray=true;}
    ;

mainParamType : name = STRING;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY
            varDecl*
            stmt*
            RETURN expr SEMI
        RCURLY
    | (PUBLIC {$isPublic=true;})?
        mainReturnType name=MAIN
        LPAREN mainParam RPAREN
        LCURLY
            varDecl*
            stmt*
        RCURLY
    ;

mainReturnType locals[boolean isArray=false]
    : STATIC name= VOID;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #BlockStmt //
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt //
    | WHILE LPAREN expr RPAREN stmt #WhileStmt //
    | expr SEMI #ExprStmt //
    | name=ID EQUALS expr SEMI #AssignStmt //
    | name=ID LBRACK expr RBRACK EQUALS expr SEMI #ArrayAssignStmt //
    ;

expr
    : LPAREN expr RPAREN #ParenExpr //
    | op=NOT expr #UnaryExpr //
    | expr op = (MUL | DIV) expr #BinaryExpr //
    | expr op = (ADD | SUB) expr #BinaryExpr //
    | expr op =LESS expr #BinaryExpr //
    | expr op=AND expr #BinaryExpr //
    | expr LBRACK expr RBRACK #ArrayAccessExpr //
    | expr PERIOD LENGTH #ArrayLengthExpr //
    | expr PERIOD name=ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr //
    | NEW INT LBRACK expr RBRACK #NewIntArrayExpr //
    | NEW name=ID LPAREN RPAREN #NewObjectExpr //
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayCreationExpr //
    | value=INTEGER #IntegerLiteral //
    | TRUE #TrueExpr //
    | FALSE #FalseExpr //
    | name=ID #VarRefExpr //
    | THIS #ThisExpr //
    ;