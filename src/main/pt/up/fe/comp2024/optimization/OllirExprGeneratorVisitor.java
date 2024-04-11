package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(THIS_LITERAL, this::visitThis);
        addVisit(NEW_EXPR, this::visitNewExpr);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitMethodCall(JmmNode node , Void unused){

        StringBuilder computation = new StringBuilder();

        StringBuilder paramCodes = new StringBuilder();

        String nameOfTheFunction = node.get("name");
        List<JmmNode> argumentsExpr = node.getChildren();

        JmmNode beforeTheDotExpr = argumentsExpr.remove(0);

        for (JmmNode argument : argumentsExpr){
            OllirExprResult argumentResult = visit(argument);
            computation.append(argumentResult.getComputation());
            paramCodes.append(", " + argumentResult.getCode());
        }

        OllirExprResult beforeTheDotResult = visit(beforeTheDotExpr);

        computation.append(beforeTheDotResult.getComputation());

        // figure out if the function is static or not
        // how? check if the beforeTheDotExpr is a varRefExpr.  if it is, check if its an existing variable(local, param or field)
        // if its not and its one of the import then we are good.
        boolean isStatic = false;

        // do verification
        if (VAR_REF_EXPR.check(beforeTheDotExpr)){
            if ((boolean) beforeTheDotExpr.getObject("isStatic")){
                isStatic = true;
            }
        }

        if (isStatic){
            return makeStaticFunctionCall(node, beforeTheDotResult.getCode(), nameOfTheFunction, paramCodes.toString());
        } else {
            return makeNonStaticFunctionCall(node, beforeTheDotResult.getCode(), nameOfTheFunction, paramCodes.toString());
        }
    }

    private OllirExprResult makeStaticFunctionCall(JmmNode node, String className, String methodName, String paramCodes) {
        StringBuilder computation = new StringBuilder();

        String type = OptUtils.toOllirType(TypeUtils.getExprType(node, table));

        if (type.equals(".V")) {
            // call the function and dont store the result
            computation.append("invokestatic(").append(className).append(", \"").append(methodName).append("\"").append(paramCodes).append(")").append(type).append(END_STMT);
            return new OllirExprResult("", computation.toString());
        }else{
            // get next temp
            String nt = OptUtils.getTemp();
            // write "nt.type :=.type invokestatic(className, methodName, paramCodes).type"
            computation.append(nt).append(type).append(SPACE).append(ASSIGN).append(type).append(SPACE);
            computation.append("invokestatic(").append(className).append(", \"").append(methodName).append("\"").append(paramCodes).append(")").append(type).append(END_STMT);

            String code = nt + type;

            return new OllirExprResult(code, computation.toString());
        }
    }

    private OllirExprResult makeNonStaticFunctionCall(JmmNode node, String className, String methodName, String paramCodes) {
        StringBuilder computation = new StringBuilder();

        // get next temp
        String nt = OptUtils.getTemp();
        String type = OptUtils.toOllirType(TypeUtils.getExprType(node, table));

        // write "nt.type :=.type invokevirtual(className, methodName, paramCodes).type"
        computation.append(nt).append(type).append(SPACE).append(ASSIGN).append(type).append(SPACE);
        computation.append("invokevirtual(").append(className).append(", \"").append(methodName).append("\"").append(paramCodes).append(")").append(type).append(END_STMT);

        String code = nt + type;

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = node.getJmmChild(0);
        var rhs = node.getJmmChild(1);
        var lhs_result = visit(lhs);
        var rhs_result = visit(rhs);

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs_result.getComputation());
        computation.append(rhs_result.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs_result.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs_result.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private boolean isImport(JmmNode node){
        // check if the variable is imported
        String variableName = node.get("name");
        var imports = table.getImports();
        for (String s : imports) {
            if (s.equals(variableName)) {
                return true;
            }
        }
        return false;
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");

        if (isImport(node)){
            return new OllirExprResult(id);
        }

        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

    private OllirExprResult visitThis(JmmNode node, Void unused) {
        return new OllirExprResult("this." + table.getClassName());
    }

    private OllirExprResult visitNewExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String className = node.get("name");
        String type = "." + className;

        // create a tmp variable to store the new object
        String nt = OptUtils.getTemp() + type;
        computation.append(nt).append(SPACE).append(ASSIGN).append(type)
                .append(SPACE).append("new(").append(className).append(")").append(type).append(END_STMT);

        // call the constructor
        computation.append("invokespecial(").append(nt).append(", \"\").V").append(END_STMT);

        // code is the temp variable
        code.append(nt);

        return new OllirExprResult(code.toString(), computation.toString());
    }

}
