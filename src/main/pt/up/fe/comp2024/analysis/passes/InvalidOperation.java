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
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
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
        var operator = binaryExpr.get("op");

        Type leftType = TypeUtils.getExprType(left, table);
        Type rightType = TypeUtils.getExprType(right, table);

        Type operatorReturnType = TypeUtils.getOperatorReturnType(operator);
        Type operatorExpectedExprsType = TypeUtils.getOperatorExprType(operator);

        // check if the type of operation is valid
        if ( (operatorExpectedExprsType.equals(leftType) || leftType == null) && (operatorExpectedExprsType.equals(rightType) ||rightType == null) ) {
            // add types to nodes
            binaryExpr.putObject("type", operatorReturnType);
            return null;
        }

        // Create error report
        var message = String.format("Operation '%s' is not valid between '%s' and '%s'.", operator, leftType, rightType);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );

        return null;
    }

    public Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        // we just need to check if the type of the unary operation is valid

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if the unary operation is valid
        var operand = unaryExpr.getChildren().get(0);
        var operator = unaryExpr.get("op");

        Type operandType = TypeUtils.getExprType(operand, table);
        Type operatorType = TypeUtils.getOperatorReturnType(operator);

        // check if the type of operation is valid
        if ( operatorType.equals(operandType) ) {
            // add the type to the unary expression
            unaryExpr.putObject("type", operatorType.getName());
            return null;
        }

        // Create error report
        var message = String.format("Operation '%s' is not valid for '%s'.", operator, operandType);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(unaryExpr),
                NodeUtils.getColumn(unaryExpr),
                message,
                null)
        );

        return null;
    }

}
