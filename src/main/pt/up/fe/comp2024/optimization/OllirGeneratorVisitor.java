package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final String L_PAREN = "(";
    private final String R_PAREN = ")";
    private final String DOUBLE_DOT = ":";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(BLOCK_STMT, this::visitBlockStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitArrayAssignStmt(JmmNode node, Void unused){
        /*
        Structure:
        code to compute index
        code to compute value
        var[index.code].type :=.type value.code;
         */

        StringBuilder code = new StringBuilder();

        // extract info
        var indexExpr = node.getJmmChild(0);
        var valueExpr = node.getJmmChild(1);
        var id = node.get("name");

        // visit exprs
        var indexRes = exprVisitor.visit(indexExpr);
        var valueRes = exprVisitor.visit(valueExpr);

        // add computation
        code.append(indexRes.getComputation());
        code.append(valueRes.getComputation());

        // get type of assigment
        Type assignType = TypeUtils.getStmtType(node, table);
        Type elementType = new Type(assignType.getName(), false);
        String assignTypeString = OptUtils.toOllirType(elementType);

        // write assignment
        code.append(id).append("[").append(indexRes.getCode()).append("]").append(assignTypeString);
        code.append(SPACE).append(ASSIGN).append(assignTypeString).append(SPACE);
        code.append(valueRes.getCode()).append(END_STMT);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode whileStmt, Void unused){
        /*
        Structure:
        goto condLabel;
        while_start:
        code to compute stmt
        condLabel:
        code to compute condition
        if condition.code goto while_start;
         */

        StringBuilder code = new StringBuilder();

        // get label
        var condLabel = OptUtils.getLabel();
        var stmtLabel = OptUtils.getLabel();

        // extract nodes
        var conditionNode = whileStmt.getJmmChild(0); // expr
        var stmtNode = whileStmt.getJmmChild(1); // stmt

        // add goto
        code.append("goto").append(SPACE).append(condLabel).append(END_STMT);

        // add label
        code.append(stmtLabel);
        code.append(DOUBLE_DOT);
        code.append(NL);

        // add stmt
        var stmt = visit(stmtNode);
        code.append(stmt);

        // add label
        code.append(condLabel);
        code.append(DOUBLE_DOT);
        code.append(NL);

        // add condition
        var condition = exprVisitor.visit(conditionNode);
        code.append(condition.getComputation());
        code.append("if").append(SPACE).append(L_PAREN); //if (
        code.append(condition.getCode()); // condition.code
        code.append(R_PAREN).append(SPACE).append("goto").append(SPACE); // ) goto
        code.append(stmtLabel).append(END_STMT); // while_start;

        return code.toString();
    }

    private String visitBlockStmt(JmmNode blockNode, Void unused) {
        StringBuilder code = new StringBuilder();

        for (var child : blockNode.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        var expr = exprVisitor.visit(node.getJmmChild(0));
        return expr.getComputation();
    }

    private String visitIfStmt(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();

        // extract ASY nodes
        var conditionNode = node.getJmmChild(0); // expr
        var thenNode = node.getJmmChild(1); // stmt
        var elseNode = node.getJmmChild(2); // stmt

        // visit them
        var condition = exprVisitor.visit(conditionNode);
        var thenRes = visit(thenNode);
        var elseRes = visit(elseNode);

        // get two labels
        var thenLabel = OptUtils.getLabel();
        var endLabel = OptUtils.getLabel();

        // add if (cond) goto thenLabel;
        code.append(condition.getComputation());
        code.append("if");
        code.append(SPACE);
        code.append(L_PAREN);
        code.append(condition.getCode());
        code.append(R_PAREN);
        code.append(SPACE);
        code.append("goto");
        code.append(SPACE);
        code.append(thenLabel);
        code.append(END_STMT);

        // add else stmt
        code.append(elseRes);

        // add goto endLabel;
        code.append("goto");
        code.append(SPACE);
        code.append(endLabel);
        code.append(END_STMT);

        // add thenLabel:
        code.append(thenLabel);
        code.append(DOUBLE_DOT);
        code.append(NL);

        // add then stmt
        code.append(thenRes);

        // add end label
        code.append(endLabel);
        code.append(DOUBLE_DOT);
        code.append(NL);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {

        // see if the var decl is local or field

        var isField = CLASS_DECL.check(node.getParent());

        if (!isField) return "";

        StringBuilder code = new StringBuilder();

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");


        code.append(".field ");
        code.append("public ");

        code.append(id);
        code.append(typeCode);
        code.append(END_STMT);

        return code.toString();
    }

    private String visitImportDecl(JmmNode node , Void unused){

        StringBuilder code = new StringBuilder();

        code.append("import ");

        @SuppressWarnings("unchecked")
        var names = (ArrayList<String>) node.getObject("names");

        for (int i = 0 ; i< names.size(); i++){
            if (i != 0){
                code.append(".");
            }
            code.append(names.get(i));
        }

        code.append(END_STMT);

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        //var lhs = exprVisitor.visit(node.getJmmChild(0));  // the left part is an ID not an Expr

        var variableName = node.get("name");

        // add to the child node the expected type of the child Expr

        var rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());


        // code to compute self
        // statement has type of lhs
        var varRefType = TypeUtils.getVarRefType(variableName, table, node.getAncestor(METHOD_DECL));
        if (varRefType== null){
            return "";
        }

        Type type = varRefType.a;
        String scope = varRefType.b;

        String typeString = OptUtils.toOllirType(type);

        if (scope.equals("field")){
            return "putfield(this, " + variableName + typeString + ", " +  rhs.getCode() + ").V" + END_STMT;
        }

        code.append(variableName);
        code.append(typeString);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);
        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        var method = node.getAncestor(METHOD_DECL);
        if (method.isEmpty()) return "";

        String methodName = node.getAncestor(METHOD_DECL).get().get("name");

        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        return id + typeCode;
    }

    private boolean isMethodVarArgs(String method){
        // check if the last param is a varArgs
        var params = table.getParameters(method);
        if (params.isEmpty()) return false;

        var lastParam = params.get(params.size()-1);

        var lastParamType = lastParam.getType();
        return (boolean) lastParamType.getObject("isVarArgs");
    }

    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");
        var name = node.get("name");

        boolean isPublic = node.get("isPublic").equals("true");

        if (isPublic) {
            code.append("public ");
        }

        if (node.get("isStatic").equals("true")) {
            code.append("static ");
        }

        if (isMethodVarArgs(name)) {
            code.append("varargs ");
        }

        // name
        code.append(name);

        StringBuilder paramsCode = new StringBuilder();
        var afterParam =0;
        // exception case for main
        if (name.equals("main")){
            var paramName = node.get("paramName");
            paramsCode.append(paramName);
            paramsCode.append(".array.String");
            afterParam = 1;
        }else {
            // params
            boolean addComma = false;
            var paramNodes = node.getChildren(PARAM);
            afterParam = paramNodes.size() +1;
            for (var param : paramNodes) {
                if (addComma) {
                    paramsCode.append(", ");
                }
                addComma = true;
                paramsCode.append(visit(param));
            }
        }
        code.append("(").append(paramsCode).append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        // if the method is void(main) we need to add a return statement
        if (name.equals("main")) {
            code.append("ret.V;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        if (table.getSuper() != null) {
            code.append(SPACE);
            code.append("extends");
            code.append(SPACE);
            code.append(table.getSuper());
        }

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
