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

    private boolean isUpcasting(Type assignType, Type childType, SymbolTable table) {
        String currentClass = table.getClassName();
        String superClass = table.getSuper();
        if (superClass != null){ // there is a super class
            return assignType.getName().equals(superClass) && childType.getName().equals(currentClass);
        }
        return false;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // cases:
        // 1. the type of both sides is the same -> valid
        // 2. the type of the right side is the current class and the type of the left side is the parent class -> valid
        // 3. the type of the right side is unknown -> invalid
        // 4. unknown case of upcasting where the assign type and child type don't match -> valid

        // for 4. we just check if the assign type isnt a primitive type because we can't upcast primitive types? and
        // if the child type is an import

        Type assignType = TypeUtils.getAssignStmtType(assignStmt, table);
        JmmNode child = assignStmt.getChildren().get(0);
        Type childType = TypeUtils.getExprType(child, table);


        if (childType == null ||
                assignType.equals(childType) ||
                isUpcasting(assignType, childType, table) ||
                (!TypeUtils.isPrimitive(assignType) && TypeUtils.isImport(childType.getName(), table))) {
            // add types to the nodes all equal to the assign type
            assignStmt.putObject("type", assignType);
            child.putObject("type", assignType);
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
