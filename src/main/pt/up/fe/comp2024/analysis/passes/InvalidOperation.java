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

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // we just need to check if both sides have the same type

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if the binary operation is valid
        var left = binaryExpr.getChildren().get(0);
        var right = binaryExpr.getChildren().get(1);

        Type leftType = TypeUtils.getExprType(left, table);
        Type rightType = TypeUtils.getExprType(right, table);

        // Check if the type of the operands is valid
        if (leftType.equals(rightType)) {
            // add information to the binaryExpr saying the type
            binaryExpr.putObject("type", leftType);
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
