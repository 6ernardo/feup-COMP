package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Objects;

public class InvalidThisExpr extends AnalysisVisitor {
    private String currentMethod;
    private boolean isCurrentMethodStatic;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS_LITERAL, this::visitThisLiteral);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        isCurrentMethodStatic = method.get("isStatic").equals("true");
        return null;
    }

    private Void visitThisLiteral(JmmNode thisLiteral, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (isCurrentMethodStatic) {
            // Create error report
            var message = "Invalid use of 'this' in a static method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(thisLiteral),
                    NodeUtils.getColumn(thisLiteral),
                    message,
                    null)
            );
        }

        return null;
    }
}
