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

public class InvalidArrayAccess extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        // check if the array access is done on an array and with an integer

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var arrayExpr = arrayAccessExpr.getChild(0);
        var indexExpr = arrayAccessExpr.getChild(1);

        var arrayType = TypeUtils.getExprType(arrayExpr, table);
        var indexType = TypeUtils.getExprType(indexExpr, table);

        if (!arrayType.isArray()) {
            // Create error report
            var message = String.format("Array access is not valid on type '%s'.", arrayType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccessExpr),
                    NodeUtils.getColumn(arrayAccessExpr),
                    message,
                    null)
            );
        }

        var IntType = new Type(TypeUtils.getIntTypeName(),false);
        if (indexType == null || !indexType.equals(IntType)){
            // Create error report
            var message = String.format("Array access index is not an integer.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccessExpr),
                    NodeUtils.getColumn(arrayAccessExpr),
                    message,
                    null)
            );
        }

        // add type to the node

        arrayAccessExpr.putObject("type", TypeUtils.getExprType(arrayAccessExpr,table));

        return null;
    }

}
