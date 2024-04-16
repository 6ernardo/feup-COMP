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

import java.util.Objects;

public class Miscellaneous extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_LENGTH_EXPR, this::visitLengthExpr);
        addVisit(Kind.TYPE, this::visitType);
    }

    private Void visitLengthExpr(JmmNode node, SymbolTable table) {
        var name = node.get("name");
        // check type of the expression
        var expr = node.getChild(0);

        var exprType = TypeUtils.getExprType(expr, table);

        if (!Objects.equals(name, "length") || !exprType.isArray()) {
            addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                "Invalid array length expression",
                null)
            );
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }


    private Void visitType(JmmNode type, SymbolTable table) {

        var typeName = type.get("name");

        if (TypeUtils.isImport(typeName, table) ||
                table.getClassName().equals(typeName) ||
                TypeUtils.isPrimitive(new Type(typeName, false))) {
            return null;
        }

        // Cretae a report
        String message = "Type " + typeName + " not found in imports or current class.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(type),
                NodeUtils.getColumn(type),
                message,
                null)
        );

        return null;
    }


}
