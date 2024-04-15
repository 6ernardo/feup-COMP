package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
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
        addVisit(BOOL_LITERAL, this::visitBool);
        addVisit(PAREN_EXPR, this::visitParenExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitParenExpr(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitBool(JmmNode node, Void unused) {
        var boolValue = node.get("name");
        String code;
        if (boolValue.equals("true")){
            code = "1.bool";
        }else{
            code = "0.bool";
        }
        return new OllirExprResult(code);
    }


    private OllirExprResult visitMethodCall(JmmNode node , Void unused){
        // progrma flow

        StringBuilder computation = new StringBuilder();
        StringBuilder paramCodes = new StringBuilder();

        // extract : name of the function, target expr and argument exprs
        String nameOfTheFunction = node.get("name");
        List<JmmNode> argumentsExpr = node.getChildren();
        JmmNode target = argumentsExpr.remove(0);

        // compute the arguments
        for (JmmNode argument : argumentsExpr){
            OllirExprResult argumentResult = visit(argument);
            computation.append(argumentResult.getComputation());
            paramCodes.append(", ").append(argumentResult.getCode());
        }

        OllirExprResult targetResult = visit(target);
        computation.append(targetResult.getComputation());

        boolean isStatic = false;

        // do verification
        if (VAR_REF_EXPR.check(target)){
            if ((boolean) target.getObject("isStatic")){
                isStatic = true;
            }
        }

        // make code like this

        String code;
        var type = OptUtils.toOllirType(TypeUtils.getExprType(node, table));

        if (type == null){
            type = getSpecialCaseType(node);
        }

        if (!type.equals(".V")){
            // get a temp variable and add "temp := " to the code
            String nt = OptUtils.getTemp();
            computation.append(nt).append(type).append(SPACE).append(ASSIGN).append(type).append(SPACE);
            code = nt + type;
        }else{
            code = "";
        }

        // make the function call

        if (isStatic){
            computation.append("invokestatic(");
        }else{
            computation.append("invokevirtual(");
        }

        computation.append(targetResult.getCode()).append(", \"").append(nameOfTheFunction).append("\"")
                .append(paramCodes).append(")").append(type).append(END_STMT);

        return new OllirExprResult(code, computation.toString());
    }

    private String getSpecialCaseType(JmmNode node) {
        // this special can occur in direct assignments or call statements
        // if its a direct assignment the type of the node is the type of the variable
        // else its void
        var parent = node.getParent();
        if (ASSIGN_STMT.check(parent)){
            return OptUtils.toOllirType(TypeUtils.getAssignStmtType(parent, table));
        }
        return ".V";
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
        // here we can have either a variable or an import in case we are calling a static function
        var id = node.get("name");

        if (isImport(node)){
            return new OllirExprResult(id); // return the import
        }

        Type type = TypeUtils.getExprType(node, table); // get the type of the variable
        String ollirType = OptUtils.toOllirType(type); // convert it to ollir type

        String var = id + ollirType; // create the code which is the name of the variable + its type

        if (isVarRefAField(node)){
            // tmp1 := getfield(this,[nameOfField].[typeOfField]).typeOfField,

            var tmpVar = OptUtils.getTemp() + ollirType;
            String computation = tmpVar +" :="+ollirType +" getfield(this," + var + ")" + ollirType + END_STMT;

            return new OllirExprResult(tmpVar, computation);
        }else{
            return new OllirExprResult(var);
        }
    }

    private boolean isVarRefAField(JmmNode node){
        return (boolean) node.getObject("isField");
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
