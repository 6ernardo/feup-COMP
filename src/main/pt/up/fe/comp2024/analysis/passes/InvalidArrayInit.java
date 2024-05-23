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

public class InvalidArrayInit extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_CREATION_EXPR, this::visitArrayCreationExpr);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpr);
    }

    private Void visitNewArrayExpr(JmmNode node, SymbolTable table) {
        var arraySize = node.getChild(0);
        // check if its an int
        var intType = new Type(TypeUtils.getIntTypeName(),false);
        var arraySizeType = TypeUtils.getExprType(arraySize, table);
        if (!intType.equals(arraySizeType)){
            // Create error report
            String message = "Invalid array size";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arraySize),
                    NodeUtils.getColumn(arraySize),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayCreationExpr(JmmNode arrayCreationExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (arrayCreationExpr.getNumChildren() == 0){
            return null;
        }
        Type arrayElemntType = TypeUtils.getExprType(arrayCreationExpr.getChild(0), table);
        if (arrayCreationExpr.getChildren().stream().anyMatch(child -> !TypeUtils.getExprType(child, table).equals(arrayElemntType))) {
            // Create error report
            String message = "Invalid initialization of array";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayCreationExpr),
                    NodeUtils.getColumn(arrayCreationExpr),
                    message,
                    null)
            );
        }

        return null;
    }

}
