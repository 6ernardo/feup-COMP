package pt.up.fe.comp2024.optimization.visitors.ConstantPropagation;

import pt.up.fe.comp.jmm.ast.*;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.Map;

public class ConstantPropagation extends AJmmVisitor<Map<String, JmmNode>, Void> {


    public boolean changed = false;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);

        setDefaultVisit(this::visitAllChildren);
    }

    private Void visitIfStmt(JmmNode node, Map<String, JmmNode> constants) {
        // get info from the node
        var info = (StmtInfo)node.getObject("info");

        var changed = info.getChanged();
        var used = info.getUsed();

        // get children
        var condition = node.getChild(0);
        var thenStmt = node.getChild(1);
        var elseStmt = node.getChild(2);

        // visit all of them with the constants table
        visit(condition, constants);
        var constantsCopy = new HashMap<>(constants);
        visit(thenStmt, constantsCopy);
        constantsCopy = new HashMap<>(constants);
        visit(elseStmt, constantsCopy);

        // now we remove the variables that were changed inside the if statement
        for (var var : changed) {
            if (constants.containsKey(var)) {
                // and if the variable is used inside the if
                // statement add the assigment back to the AST
                var assignStmt = constants.get(var);
                node.insertBefore(assignStmt);
                constants.remove(var);

            }
        }

        return null;

    }

    private Void visitWhileStmt(JmmNode node, Map<String, JmmNode> constants) {
        // get info from the node
        var info = (StmtInfo)node.getObject("info");

        var changed = info.getChanged();
        var used = info.getUsed();

        // remove from the constants the values changed inside the while loop
        for (var var : changed){
            if (constants.containsKey(var)){
                // and if the variable is used inside the while loop add the assigment back to the AST
                var assignStmt = constants.get(var);
                node.insertBefore(assignStmt);
                constants.remove(var);
            }
        }

        // make a copy of the map to avoid changing the original map
        var constantsCopy = new HashMap<>(constants);
        // visit children
        visitAllChildren(node, constantsCopy);
        return null;
    }

    private Void visitMethodExpr(JmmNode methodDecl, Map<String, JmmNode> constants) {
        // clear the constantValues map when entering a new method
        visitAllChildren(methodDecl, new HashMap<>());
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt,Map<String, JmmNode> constants) {
        // visit all children first
        visitAllChildren(assignStmt, constants);

        var varName = assignStmt.get("name");
        var expr = assignStmt.getChild(0);
        if (Kind.INTEGER_LITERAL.check(expr) || Kind.BOOL_LITERAL.check(expr)){
            // add the value to the constantValues map
            // make a copy of the node to avoid changing the original node

            constants.put(varName, assignStmt);

            // remove the assignment from the AST
            assignStmt.detach();

        } else{ // remove the value from the constantValues map cause it's not a constant anymore
            constants.remove(varName);
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, Map<String, JmmNode> constants) {
        var varName = varRefExpr.get("name");
        if (constants.containsKey(varName)){
            var assignStmt = constants.get(varName);

            // get the value from the assignment
            var value = assignStmt.getChild(0);

            // make a copy
            var valueCopy = value.copy();

            // replace the node
            varRefExpr.replace(valueCopy);

            changed = true;
        }
        return null;
    }


}
