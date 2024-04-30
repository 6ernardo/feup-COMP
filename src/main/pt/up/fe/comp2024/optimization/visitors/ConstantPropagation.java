package pt.up.fe.comp2024.optimization.visitors;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.Map;

public class ConstantPropagation extends PreorderJmmVisitor<SymbolTable, Void> {

    private Map<String, Integer> constantValues;
    public boolean changed = false;

    public ConstantPropagation() {
        this.constantValues = new HashMap<>();
    }
    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodExpr);

        setDefaultVisit((node, symbolTable) -> null);
    }

    private Void visitMethodExpr(JmmNode methodDecl, SymbolTable table) {
        // clear the constantValues map when entering a new method
        constantValues = new HashMap<>();
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var varName = assignStmt.get("name");
        var expr = assignStmt.getChild(0);
        if (Kind.INTEGER_LITERAL.check(expr)){ // add the value to the constantValues map
            var value = expr.get("value");
            // check if the value can be parsed to an integer
            int valueInt;
            try{
                valueInt = Integer.parseInt(value);
            } catch (NumberFormatException e){
                return null;
            }
            constantValues.put(varName, valueInt);
            // remove node from the tree
            assignStmt.detach();
            changed = true;
        } else{ // remove the value from the constantValues map cause it's not a constant anymore
            constantValues.remove(varName);
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        var varName = varRefExpr.get("name");
        if (constantValues.containsKey(varName)){
            var value = constantValues.get(varName);

            // change the AST - replace the VarRefExpr node with an IntegerLiteral node
            var newIntLiteral = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());

            newIntLiteral.put("value", String.valueOf(value));
            // replace the node
            varRefExpr.replace(newIntLiteral);

            changed = true;
        }
        return null;
    }



}
