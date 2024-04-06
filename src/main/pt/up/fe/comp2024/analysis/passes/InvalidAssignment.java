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
 * Checks if the initialization of a variable is valid.
 *
 */

public class InvalidAssignment extends AnalysisVisitor {
    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        Type assignType = TypeUtils.getAssignStmtType(assignStmt, table);
        Type childType = TypeUtils.getExprType(assignStmt.getChildren().get(0), table);

        if (assignType.equals(childType)) {
            return null;
        }

        // check if : rhs type is the current class and lhs type is the parent class

        // first get superclass
        String superClass = table.getSuper();
        // get current class
        String currentClass = table.getClassName();

        if (superClass != null) {
            // check if the assignType is the superClass
            if (assignType.getName().equals(superClass) && childType.getName().equals(currentClass)) {
                // we have a case of upcasting
                return null;
            }
        }

        // if both types are imports then we can't check
        if (table.getImports().stream().anyMatch(imported -> imported.equals(assignType.getName())) &&
                table.getImports().stream().anyMatch(imported -> imported.equals(childType.getName()))) {
            return null;
        }


        // Create error report
        String message = "Invalid assignment of variable";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignStmt),
                NodeUtils.getColumn(assignStmt),
                message,
                null)
        );


        return null;
    }

}
