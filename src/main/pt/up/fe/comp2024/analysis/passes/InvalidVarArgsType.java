package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class InvalidVarArgsType extends AnalysisVisitor {
    private String currentMethod;
    private String lastParameterName;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.TYPE, this::visitType);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        if (currentMethod.equals("main")) {
            lastParameterName = method.get("paramName");
        }else{
            var params = method.getChildren(Kind.PARAM);
            if (!params.isEmpty()) {
                lastParameterName = params.get(params.size() - 1).get("name");
            } else {
                lastParameterName = null;
            }
        }
        return null;
    }

    private Void visitType(JmmNode type, SymbolTable table) {
        // check if type is of varargs
        if (type.get("isVarArgs").equals("false")) {
            return null;
        }

        // get parent node

        var parent = type.getParent();

        // check kind

        if (Kind.PARAM.check(parent)){
            // we check if the param is the last one and if it is an array
            SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
            if (lastParameterName != null && !lastParameterName.equals(parent.get("name"))) {
                // Create error report
                var message = "Invalid use of varargs in parameter " + lastParameterName;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(type),
                        NodeUtils.getColumn(type),
                        message,
                        null)
                );
            }
        }else if (Kind.VAR_DECL.check(parent)){
            // Create error report
            var message = "Invalid use of varargs in variable declaration";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(type),
                    NodeUtils.getColumn(type),
                    message,
                    null)
            );
        } else if (Kind.METHOD_DECL.check(parent)){
            // Create error report
            var message = "Invalid use of varargs in return type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(type),
                    NodeUtils.getColumn(type),
                    message,
                    null)
            );
        }
        return null;
    }


}
