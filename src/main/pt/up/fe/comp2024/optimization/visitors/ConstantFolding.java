package pt.up.fe.comp2024.optimization.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;
import java.util.Map;

public class ConstantFolding extends PreorderJmmVisitor<SymbolTable, Void> {

    public boolean changed = false;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);

        setDefaultVisit((node, symbolTable) -> null);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var varName = assignStmt.get("name");
        var expr = assignStmt.getChild(0);
        if (Kind.BINARY_EXPR.check(expr)){
            // Check if the binary expr is with 2 Integer Literals
            var child1 = expr.getChild(0);
            var child2 = expr.getChild(1);
            if (Kind.INTEGER_LITERAL.check(child1) && Kind.INTEGER_LITERAL.check(child2)) {
                // calculate new value
                var value1 = Integer.parseInt(child1.get("value"));
                var value2 = Integer.parseInt(child2.get("value"));
                var operator = expr.get("op");
                var newValue = calculateValue(value1, value2, operator);

                // create new Integer Literal node
                var newIntLiteral = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
                newIntLiteral.put("value", String.valueOf(newValue));

                // create a list with the new Integer Literal node
                List<JmmNode> newChildren = List.of(newIntLiteral);
                assignStmt.setChildren(newChildren);
                changed = true;
            }
        }
        return null;
    }

    private int calculateValue(int value1, int value2, String operator) {
        switch (operator) {
            case "+":
                return value1 + value2;
            case "-":
                return value1 - value2;
            case "*":
                return value1 * value2;
            case "/":
                return value1 / value2;
            default:
                return 0;
        }
    }
}
