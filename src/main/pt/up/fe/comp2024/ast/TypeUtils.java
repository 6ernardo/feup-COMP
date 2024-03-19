package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.PARAM;

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

    // and these
    public static String getBooleanTypeName() {return BOOLEAN_TYPE_NAME;}
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
            case ASSIGN_STMT -> getAssignStmtType(stmt, table);
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

        throw new RuntimeException("Variable '" + variableName + "' not found in the symbol table");
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
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
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
}
