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

public class IncompatibleReturn extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");


        // check if the type of the return statement is compatible with the method return type

        var methordReturnType = table.getReturnType(currentMethod);

        var returnExpr = returnStmt.getChild(0);

        var returnType = TypeUtils.getExprType(returnExpr, table);

        // see if they are the same type


        var isSameType = true; // assume the unknown type is the same

        if (returnType != null){
            isSameType =  methordReturnType.equals(returnType);
        }

        if (!isSameType) {
            // Create error report
            var message = "Incompatible return type. Expected " + methordReturnType + " but got " + returnType;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
