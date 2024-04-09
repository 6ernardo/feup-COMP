package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class IncompatibleArguments extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {

        // Check all the params
        for (int i = 1; i < methodCall.getChildren().size(); i++) {
            var paramNode = methodCall.getChildren().get(i);
            var paramType = TypeUtils.getParamType(methodCall, i - 1, table);
            var localType = TypeUtils.getVarExprType(paramNode, table);

            if (paramType != localType) {
                // Create error report
                var message = "Incompatible argument type. Expected " + methodCall + " but got " + methodCall;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        message,
                        null)
                );
            }
        }

        return null;
    }
}
