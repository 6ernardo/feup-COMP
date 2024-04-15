package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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

        // Method is a declared method, return
        var methodDeclName = methodCall.get("name");
        if (table.getMethods().stream()
                .noneMatch(methodDecl -> methodDecl.equals(methodDeclName))) {
            return null;
        }

        // check if the number of parameters exceeds the number of arguments
        var parameters = table.getParameters(methodCall.get("name"));
        var arguments = methodCall.getChildren();

        if (arguments.size()-1 < parameters.size()){ // check if there were too little arguments
            var message = "Method " + methodCall.get("name") + " requires " + parameters.size() +
                    " arguments but only " + (arguments.size()-1) + " were provided.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    message,
                    null)
            );
            return null;
        }

        boolean excessArgs = false;
        if (arguments.size()-1 > parameters.size()){
            if (!(boolean) parameters.get(parameters.size()-1).getType().getObject("isVarArgs")){
                // check if there were too many arguments
                var message = "Method " + methodCall.get("name") + " requires only " + parameters.size() +
                        " arguments but " + (arguments.size() - 1) + " were provided.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall),
                        message,
                        null)
                );
                return null;
            }else{
                excessArgs = true;
            }
        }

        // Check all the params
        for (int i = 1; i < arguments.size(); i++) {
            var argNode = arguments.get(i);
            Type argType = TypeUtils.getExprType(argNode, table);

            if (i-1 >= parameters.size()-1 && excessArgs){ // all the excess arguments plus one
                var paramType = parameters.get(parameters.size()-1).getType();
                var paramElementType = new Type(paramType.getName(), false);
                if (!paramElementType.equals(argType)) {
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
                continue;
            }
            var paramType = parameters.get(i-1).getType();

            if (!paramType.equals(argType)) {
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
