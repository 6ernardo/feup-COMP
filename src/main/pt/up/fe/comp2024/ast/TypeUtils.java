package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String STRING_TYPE_NAME = "String";
    private static final String VOID_TYPE_NAME = "void";
    private static final String INT_ARRAY_TYPE_NAME = "int[]";
    private static final String INT_ELLIPSIS_TYPE_NAME = "int...";


    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public static String getStringTypeName() {
        return STRING_TYPE_NAME;
    }
    public static String getVoidTypeName() {
        return VOID_TYPE_NAME;
    }
    public static String getIntArrayTypeName() {
        return INT_ARRAY_TYPE_NAME;
    }
    public static String getIntEllipsisTypeName() {
        return INT_ELLIPSIS_TYPE_NAME;
    }

    public static Type getStmtType(JmmNode stmt, SymbolTable table) {
        var kind = Kind.fromString(stmt.getKind());

        Type type = switch (kind) {
            case ASSIGN_STMT, ARRAY_ASSIGN_STMT -> getAssignStmtType(stmt, table);
            default -> throw new UnsupportedOperationException("Can't compute type for statement kind '" + kind + "'");
        };

        return type;
    }




    private static Type getVariableType(String variableName, SymbolTable table ,  Optional<JmmNode> currentMethod){
        // check if there is an entry for the variable in the method
        if (currentMethod.isPresent()) {
            var currentMethodNode = currentMethod.get();
            List<Symbol> methodsLocals = table.getLocalVariables(currentMethodNode.get("name"));
            for (Symbol s : methodsLocals) {
                if (s.getName().equals(variableName)) {
                    return s.getType();
                }
            }
            // check if the variable is a parameter
            var params = table.getParameters(currentMethodNode.get("name"));
            for (Symbol s : params) {
                if (s.getName().equals(variableName)) {
                    // check if its a varargs and if so return array type instead
                    return s.getType();
                }
            }
        }
        // check if the variable is a field
        var fields = table.getFields();
        for (Symbol s : fields) {
            if (s.getName().equals(variableName)) {
                return s.getType();
            }
        }

        // check if the variable is imported
        var imports = table.getImports();
        for (String s : imports) {
            if (s.equals(variableName)) {
                return new Type(variableName, false);
            }
        }

        return null;
    }

    public static Type getAssignStmtType(JmmNode assignStmt, SymbolTable table) {
        // go through the table to find the type of the variable
        var varName = assignStmt.get("name");

        // get the type of the variable through the table

        // check if we are in a method and throw an exception if we are not
        var method = assignStmt.getAncestor(METHOD_DECL);
        if (method.isEmpty()) {
            throw new RuntimeException("Assign statement not inside a method");
        }

        return getVariableType(varName, table, method);
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL, ARRAY_LENGTH_EXPR -> new Type(INT_TYPE_NAME, false);
            case PAREN_EXPR -> getExprType(expr.getChild(0), table);
            case UNARY_EXPR, BOOL_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case ARRAY_ACCESS_EXPR -> getArrayElementType(expr, table);
            case METHOD_CALL_EXPR -> getMethodReturnType(expr, table);
            case NEW_EXPR -> new Type(expr.get("name"), false);
            case ARRAY_CREATION_EXPR -> getArrayInitType(expr, table);
            case THIS_LITERAL -> getThisType(expr, table);
            case NEW_ARRAY_EXPR -> getNewArrayExpr(expr,table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getNewArrayExpr(JmmNode expr, SymbolTable table) {
        var type = expr.get("name");
        return new Type(type, true);
    }

    private static Type getThisType(JmmNode thisLiteral, SymbolTable table) {
        // get the class name from the symbol table
        return new Type(table.getClassName(), false);
    }

    private static Type getArrayInitType(JmmNode arrayCreationExpr, SymbolTable table) {
        var type = getExprType(arrayCreationExpr.getChild(0), table);
        return new Type(type.getName(), true);
    }

    private static Type getArrayElementType(JmmNode arrayAccessExpr, SymbolTable table) {
        var type = getExprType(arrayAccessExpr.getChild(0), table);
        if (!type.isArray()){
            return null;
        }
        return new Type(type.getName(), false);
    }

    private static Type getMethodReturnType(JmmNode methodCallExpr, SymbolTable table) {
        var targetNode = methodCallExpr.getChild(0);
        var targetType = getExprType(targetNode, table);

        if (targetType == null) {
            return null;
        }

        if (!targetType.getName().equals(table.getClassName())){
            return null;
        }
        var methodName = methodCallExpr.get("name");
        return table.getReturnType(methodName);
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "*", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // Check varRefExpr is integer literal
        if (varRefExpr.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            return new Type("int", false);
        }

        var varName = varRefExpr.get("name");

        // check if we are in a method and what method
        var method = varRefExpr.getAncestor(METHOD_DECL);

        return getVariableType(varName, table, method);
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }

    public static Type getOperatorReturnType(String operator) {
        return switch (operator) {
            case "+", "-", "*", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", "&&", "!" -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new RuntimeException("Unknown operator '" + operator + "'");
        };
    }

    public static Type getOperatorExprType(String operator){
        return switch (operator) {
            case "+", "-", "*", "/", "<" -> new Type(INT_TYPE_NAME, false);
            case "&&", "not" -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new RuntimeException("Unknown operator '" + operator + "'");
        };
    }

    public static Type getParamType(JmmNode methodCallExpr, int paramIndex, SymbolTable table) {
        var methodName = methodCallExpr.get("name");

        // check if method has params
        if (table.getParameters(methodName) == null) {
            throw new RuntimeException("Method has no parameters");
        }

        // Check if the param is varargs, if so return the varargs type
        if (paramIndex >= table.getParameters(methodName).size() - 1) {
            var lastParam = table.getParameters(methodName).get(table.getParameters(methodName).size() - 1);
            if ((Boolean) lastParam.getType().getObject("isVarArgs")) {
                return lastParam.getType();
            }
        }

        // check if paramIndex overflow
        if (paramIndex > table.getParameters(methodName).size() - 1) {
            throw new RuntimeException("Parameter index overflow");
        }

        return table.getParameters(methodName).get(paramIndex).getType();
    }


    public static boolean isVariable(String name , String methodSignature, SymbolTable table){


        var locals = table.getLocalVariables(methodSignature);
        if (locals != null){
            for (Symbol s : locals){
                if (s.getName().equals(name)){
                    return true;
                }
            }
        }


        var params = table.getParameters(methodSignature);
        if (params != null){
            for (Symbol s : params){
                if (s.getName().equals(name)){
                    return true;
                }
            }
        }

        var fields = table.getFields();
        if (fields != null){
            for (Symbol s : fields){
                if (s.getName().equals(name)){
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isImport(String name, SymbolTable table){

        var imports = table.getImports();
        for (String s : imports) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimitive(Type assignType) {
        return assignType.getName().equals(INT_TYPE_NAME) || assignType.getName().equals(BOOLEAN_TYPE_NAME);
    }

    public static boolean isField(String name , String methodSignature, SymbolTable table){
        var locals = table.getLocalVariables(methodSignature);
        if (locals != null){
            for (Symbol s : locals){
                if (s.getName().equals(name)){
                    return false;
                }
            }
        }


        var params = table.getParameters(methodSignature);
        if (params != null){
            for (Symbol s : params){
                if (s.getName().equals(name)){
                    return false;
                }
            }
        }

        var fields = table.getFields();
        if (fields != null){
            for (Symbol s : fields){
                if (s.getName().equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

    public static Type getElementType(Type arrayType) {
        return new Type(arrayType.getName(), false);
    }

}
