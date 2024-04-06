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

public class UndefinedMethod extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCallExpr);
    }

    private Void visitMethodCallExpr(JmmNode methodCall, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var methodName = methodCall.get("name");

        // get the type of the object form where the method is called
        var object = methodCall.getChild(0);
        var objectType = TypeUtils.getExprType(object, table);

        if (objectType == null) // unknown type means there is an import involved so we can't check
            return null;

        // check if its an import
        if (table.getImports().stream()
                .anyMatch(imported -> imported.equals(objectType.getName()))) {
            return null;
        }

        // check if type is current class
        if (objectType.getName().equals(table.getClassName())){
            // check if the method is declared in the class
            if (table.getMethods().stream()
                    .anyMatch(method -> method.equals(methodName))) {
                return null;
            }
        }

        // check if its a superclass
        var superClass = table.getSuper();
        if (superClass != null) {
            if (objectType.getName().equals(superClass) || objectType.getName().equals(table.getClassName())) {
                return null;
            }
        }

        var message = String.format("Method '%s' is not declared.", methodName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(methodCall),
                NodeUtils.getColumn(methodCall),
                message,
                null)
        );

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
}
