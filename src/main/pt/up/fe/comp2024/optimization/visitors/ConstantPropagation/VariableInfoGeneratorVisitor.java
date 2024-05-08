package pt.up.fe.comp2024.optimization.visitors.ConstantPropagation;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

public class VariableInfoGeneratorVisitor extends AJmmVisitor<Void, StmtInfo> {

    @Override
    protected void buildVisitor() {
        addVisit(Kind.WHILE_STMT, this::storeInfoInNode);
        addVisit(Kind.IF_STMT, this::storeInfoInNode);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private StmtInfo visitVarRefExpr(JmmNode varRefExpr, Void unused){
        // get the variable name
        var varName = varRefExpr.get("name");
        // create a new StmtInfo with the used variable
        var result = new StmtInfo();
        result.addUsed(varName);
        return result;
    }

    private StmtInfo visitAssignStmt(JmmNode assignStmt, Void unused){
        // get the variable name and the expression
        var varName = assignStmt.get("name");
        var expr = assignStmt.getChild(0);
        // visit the expression to get the used variables
        var exprInfo = visit(expr, null);
        // create a new StmtInfo with the changed variable and the used variables
        var result = new StmtInfo();
        result.addChanged(varName);
        result.addUsed(exprInfo.getUsed());
        return result;
    }

    private StmtInfo storeInfoInNode(JmmNode node, Void unused){
        // perform the standard visit but store the result in the node
        var result = defaultVisit(node, unused);
        node.putObject("info", result);
        return result;
    }

    private StmtInfo defaultVisit(JmmNode node, Void unused) {
        // visit all children and combine the results
        var result = new StmtInfo();
        for (var child : node.getChildren()) {
            var childInfo = visit(child, null);
            result.addChanged(childInfo.getChanged());
            result.addUsed(childInfo.getUsed());
        }
        return result;
    }
}
