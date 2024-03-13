package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
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

        computation.append( "invokestatic(" + beforeTheDotResult.getCode() + ", \"" + nameOfTheFunction + "\"" +paramCodes + ").V");

        return new OllirExprResult("", computation.toString());
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

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

}
