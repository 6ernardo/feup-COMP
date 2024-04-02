package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class InvalidConditionExpr extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_STMT,this::visitConditionExpr);
        addVisit(Kind.WHILE_STMT,this::visitConditionExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitConditionExpr(JmmNode stmt, SymbolTable table) {
        // check if the condition is a boolean expression
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var condition = stmt.getChild(0);
        var conditionType = TypeUtils.getExprType(condition, table);

        if (!conditionType.getName().equals(TypeUtils.getBooleanTypeName())) {
            // Create error report
            var message = String.format("Condition expression is not a boolean.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    message,
                    null)
            );

        }

        return null;
    }

}

