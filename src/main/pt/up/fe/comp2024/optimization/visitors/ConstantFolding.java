package pt.up.fe.comp2024.optimization.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Map;

public class ConstantFolding extends PostorderJmmVisitor<Void, Void> {

    public boolean changed = false;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);

        setDefaultVisit((node, symbolTable) -> null);
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, Void unused) {
        // Check if the unary expr is with an Integer Literal
        var child = unaryExpr.getChild(0);
        if (Kind.BOOL_LITERAL.check(child)) {
            // calculate new value
            var value = Boolean.parseBoolean(child.get("value"));
            var operator = unaryExpr.get("op");
            var newValue = calculateValue(value, operator);

            // create new Boolean Literal node
            var newBoolLiteral = Kind.BOOL_LITERAL.createNode();
            newBoolLiteral.put("value",newValue);

            unaryExpr.replace(newBoolLiteral);
            changed = true;
        }
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        // Check if the binary expr is with 2 Integer Literals
        var child1 = binaryExpr.getChild(0);
        var child2 = binaryExpr.getChild(1);
        if (Kind.INTEGER_LITERAL.check(child1) && Kind.INTEGER_LITERAL.check(child2)) {
            // calculate new value
            var value1 = Integer.parseInt(child1.get("value"));
            var value2 = Integer.parseInt(child2.get("value"));
            var operator = binaryExpr.get("op");
            var newValue = calculateValue(value1, value2, operator);

            var opRtnType = TypeUtils.getOperatorReturnType(operator);

            Kind newNodeKind;
            if (opRtnType.equals(new Type(TypeUtils.getIntTypeName(), false))){
                newNodeKind = Kind.INTEGER_LITERAL;
            } else if (opRtnType.equals(new Type(TypeUtils.getBooleanTypeName(), false))){
                newNodeKind = Kind.BOOL_LITERAL;
            } else {
                return null;
            }


            // create new Integer Literal node
            var newIntLiteral = newNodeKind.createNode();
            newIntLiteral.put("value",newValue);

            // create a list with the new Integer Literal node
            List<JmmNode> newChildren = List.of(newIntLiteral);
            binaryExpr.replace(newIntLiteral);
            changed = true;
        }
        if (Kind.BOOL_LITERAL.check(child1) && Kind.BOOL_LITERAL.check(child2)) {
            // calculate new value
            var value1 = Boolean.parseBoolean(child1.get("value"));
            var value2 = Boolean.parseBoolean(child2.get("value"));
            var operator = binaryExpr.get("op");
            var newValue = calculateValue(value1, value2, operator);

            // create new Boolean Literal node
            var newBoolLiteral = new JmmNodeImpl(Kind.BOOL_LITERAL.toString());
            newBoolLiteral.put("value",newValue);

            // create a list with the new Boolean Literal node
            List<JmmNode> newChildren = List.of(newBoolLiteral);
            binaryExpr.replace(newBoolLiteral);
            changed = true;
        }
        return null;
    }

    private String calculateValue(boolean value, String operator) {
        if (operator.equals("not")) {
            return String.valueOf(!value);
        }
        return "";
    }

    private String calculateValue(int value1, int value2, String operator) {
        switch (operator) {
            case "+":
                return String.valueOf(value1+value2);
            case "-":
                return String.valueOf(value1-value2);
            case "*":
                return String.valueOf(value1*value2);
            case "/":
                return String.valueOf(value1/value2);
            case "<":
                return String.valueOf(value1<value2);
            default:
                return "";
        }
    }

    private String calculateValue(boolean value1, boolean value2, String operator) {
        switch (operator) {
            case "&&":
                return String.valueOf(value1 && value2);
            case "||":
                return String.valueOf(value1 || value2);
            default:
                return "";
        }
    }
}
