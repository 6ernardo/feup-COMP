package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the binary operation between two operands is valid.
 *
 */

public class InvalidOperation extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private boolean isInteger(JmmNode varRefExpr, SymbolTable table) {
        // Find the variable in the local variables of the current method
        Type varType = TypeUtils.getExprType(varRefExpr, table);

        // Check if the variable is an integer
        if (!varType.getName().equals("int")) {
            return false;
        }

        // Check if the variable is an array
        if (varType.isArray()) {
            return false;
        }

        return true;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if the binary operation is valid
        var left = binaryExpr.getChildren().get(0);
        var right = binaryExpr.getChildren().get(1);

        // If the left and right operand is an integer literal or int variable
        if (isInteger(left, table) && isInteger(right,table)){
            return null;
        }

        // Create error report
        var message = String.format("Invalid operation between '%s' and '%s'.", left.getKind(), right.getKind());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );

        return null;
    }

}
