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


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(EXPR_STMT, this::visitExprStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        var expr = exprVisitor.visit(node.getJmmChild(0));
        return expr.getComputation();
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
        Type thisType = TypeUtils.getStmtType(node, table);
        String typeString = OptUtils.toOllirType(thisType);

        if (isAssigningField(node)){
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

    private boolean isAssigningField(JmmNode node){
        var methodNode = node.getParent();
        var methodName = methodNode.get("name");
        var assignName = node.get("name");

        // check if its a param or local variable first

        if(table.getLocalVariables(methodName).stream().anyMatch(
                sym -> sym.getName().equals(assignName)
        )){
            return false;
        }

        if(table.getParameters(methodName).stream().anyMatch(
                sym -> sym.getName().equals(assignName)
        )){
            return false;
        }

        return table.getFields().stream().anyMatch(
                sym -> sym.getName().equals(assignName)
        );
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
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

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");
        var name = node.get("name");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (name.equals("main")) {
            code.append("static ");
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
