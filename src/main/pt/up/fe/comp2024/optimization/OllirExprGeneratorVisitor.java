package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private static final String END_STMT = ";\n";

    private static final String LEFT_PAREN = "(";
    private static final String RIGHT_PAREN = ")";

    private static final String NEW_LINE = "\n";

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
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);
        addVisit(ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(ARRAY_CREATION_EXPR, this::visitArrayInitExpr);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get the expression
        var expr = node.getJmmChild(0);

        // get operator
        var operator = node.get("op");

        // visit the expression
        OllirExprResult exprResult = visit(expr);
        computation.append(exprResult.getComputation());

        // get the type of the expression
        Type type = TypeUtils.getOperatorReturnType(operator);

        // get the ollir type
        String ollirType = OptUtils.toOllirType(type);

        // get a temp variable
        String tempVar = OptUtils.getTemp();

        // add the instruction
        computation.append(tempVar).append(ollirType).append(SPACE);
        computation.append(ASSIGN).append(ollirType).append(SPACE);
        computation.append(operator).append(ollirType).append(SPACE);
        computation.append(exprResult.getCode()).append(END_STMT);

        // add the temp variable to the code
        code.append(tempVar).append(ollirType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayInitExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get the type of the array
        Type type = TypeUtils.getExprType(node, table);
        Type elementType = new Type(type.getName(), false);
        String ollirType = OptUtils.toOllirType(elementType);

        // get the expressions
        List<JmmNode> exprs = node.getChildren();

        // create the array
        OllirExprResult arrayResult = createArray(exprs, elementType);
        computation.append(arrayResult.getComputation());

        // add the array to the code
        code.append(arrayResult.getCode());

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult createArray(List<JmmNode> exprs, Type elementType){
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get the ollir type
        String ollirType = OptUtils.toOllirType(elementType);

        // create the array
        String arrayVar = OptUtils.getTemp();
        computation.append(arrayVar).append(".array").append(ollirType);
        computation.append(SPACE);
        computation.append(ASSIGN).append(".array").append(ollirType);
        computation.append(SPACE);
        computation.append("new(array, ").append(exprs.size()).append(".i32").append(").array").append(ollirType);
        computation.append(END_STMT);

        // assign the values to the array
        for (int i = 0; i < exprs.size(); i++){
            OllirExprResult exprResult = visit(exprs.get(i));
            computation.append(exprResult.getComputation());
            computation.append(arrayVar).append("[").append(i)
                    .append(".i32").append("]").append(ollirType).append(SPACE);
            computation.append(ASSIGN).append(ollirType).append(SPACE).append(exprResult.getCode()).append(END_STMT);
        }

        // add the array to the code
        code.append(arrayVar).append(".array").append(ollirType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayAccessExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get temp variable
        String tempVar = OptUtils.getTemp();

        // extract expressions
        var arrayExpr = node.getJmmChild(0);
        var indexExpr = node.getJmmChild(1);

        // visit the expressions
        OllirExprResult arrayResult = visit(arrayExpr);
        OllirExprResult indexResult = visit(indexExpr);

        // add computation
        computation.append(indexResult.getComputation());
        computation.append(arrayResult.getComputation());

        // get type of element in array
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        // add instruction : tmp.type :=.type var.array.type[index.i32].type;
        computation.append(tempVar).append(ollirType).append(SPACE);
        computation.append(ASSIGN).append(ollirType).append(SPACE);
        computation.append(arrayResult.getCode()).append("[");
        computation.append(indexResult.getCode()).append("]").append(ollirType).append(END_STMT);

        // add temp variable to code
        code.append(tempVar).append(ollirType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayLengthExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get the array expression
        var arrayExpr = node.getJmmChild(0);

        // visit the array expression
        OllirExprResult arrayResult = visit(arrayExpr);
        computation.append(arrayResult.getComputation());

        // add instruction : tmp.i32 :=.i32 arraylength(arrayResult.code).i32;
        String tempVar = OptUtils.getTemp();
        computation.append(tempVar).append(".i32").append(SPACE).append(ASSIGN).append(".i32").append(SPACE)
                .append("arraylength(").append(arrayResult.getCode()).append(").i32").append(END_STMT);

        // add temp variable to code
        code.append(tempVar).append(".i32");

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // extract node and type
        var sizeExpr = node.getJmmChild(0);
        var type = new Type(node.get("name"), false);

        // compute the size
        OllirExprResult sizeResult = visit(sizeExpr);
        computation.append(sizeResult.getComputation());

        // get temp variable
        String tempVar = OptUtils.getTemp();
        String ollirType = OptUtils.toOllirType(type);

        // create the array with intruction : tmp.array.type :=.array.type new(array,size.i32).array.type
        computation.append(tempVar).append(".array").append(ollirType);
        computation.append(SPACE);
        computation.append(ASSIGN).append(".array").append(ollirType);
        computation.append(SPACE);
        computation.append("new(array, ").append(sizeResult.getCode()).append(").array").append(ollirType);
        computation.append(END_STMT);

        // add temp variable to code
        code.append(tempVar).append(".array").append(ollirType);
        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitParenExpr(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitBool(JmmNode node, Void unused) {
        var boolValue = node.get("value");
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
        var isVarArgsUsed = NodeUtils.getBooleanAttribute(node, "isVarArgsUsed", "false");

        if (isVarArgsUsed){
            // if varargs is used we need to remove the last argument
            var argumentsSize = argumentsExpr.size();
            var parameters = table.getParameters(nameOfTheFunction);
            var parametersSize = parameters.size();

            for (int i = 0; i < parametersSize-1; i++){
                OllirExprResult argumentResult = visit(argumentsExpr.get(i));
                computation.append(argumentResult.getComputation());
                paramCodes.append(", ").append(argumentResult.getCode());
            }

            // get the last argument type
            var lastArgumentType = parameters.get(parametersSize-1).getType();
            var elementType = new Type(lastArgumentType.getName(), false);
            var listOfExprs = argumentsExpr.subList(parametersSize-1, argumentsSize);

            OllirExprResult arrayResult = createArray(listOfExprs, elementType);

            computation.append(arrayResult.getComputation());
            paramCodes.append(", ").append(arrayResult.getCode());

        }else {
            for (JmmNode argument : argumentsExpr) {
                OllirExprResult argumentResult = visit(argument);
                computation.append(argumentResult.getComputation());
                paramCodes.append(", ").append(argumentResult.getCode());
            }
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

        var isImportedClass = (boolean)node.getObject("isTargetAImport");

        // make code like this

        String code;

        String type = null;
        if (!isImportedClass){
            type = OptUtils.toOllirType(TypeUtils.getExprType(node, table));
        }

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
            var assignmentType = TypeUtils.getAssignStmtType(parent, table);
            return OptUtils.toOllirType(assignmentType);
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

        // get operator
        String op = node.get("op");
        var opRtrnType = TypeUtils.getOperatorReturnType(op);

        if (op.equals("&&") || op.equals("||")){
            return visitShortCircuitExpr(node, lhs_result, rhs_result,op);
        }

        // code to compute the children
        computation.append(lhs_result.getComputation());
        computation.append(rhs_result.getComputation());

        // code to compute self
        String resOllirType = OptUtils.toOllirType(opRtrnType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs_result.getCode()).append(SPACE)
                .append(op).append(resOllirType).append(SPACE)
                .append(rhs_result.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitShortCircuitExpr(JmmNode node, OllirExprResult lhsResult,
                                                  OllirExprResult rhsResult, String op) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // get labels
        String endLabel = OptUtils.getLabel();

        // get tmp variable
        String tmpVar = OptUtils.getTemp() + ".bool";

        // add computation of lhs
        computation.append(lhsResult.getComputation());

        if (op.equals("&&")) {
            // get another label
            String andLabel = OptUtils.getLabel();
            computation.append("if ")
                    .append(LEFT_PAREN).append(lhsResult.getCode()).append(RIGHT_PAREN)
                    .append(" goto ").append(andLabel).append(END_STMT);
            computation.append(tmpVar).append(SPACE).append(ASSIGN).append(".bool")
                    .append(SPACE).append("0.bool").append(END_STMT);
            computation.append("goto ").append(endLabel).append(END_STMT);
            computation.append(andLabel).append(":").append(NEW_LINE);
        } else {
            // assign true to tmpVar
            // TODO: check if there is a better way to do this with less assignments
            computation.append(tmpVar).append(SPACE).append(ASSIGN).append(".bool")
                    .append(SPACE).append("1.bool").append(END_STMT);
            computation.append("if ")
                    .append(LEFT_PAREN).append(lhsResult.getCode()).append(RIGHT_PAREN)
                    .append(" goto ").append(endLabel).append(END_STMT);
        }

        // add computation of rhs
        computation.append(rhsResult.getComputation());

        // assign the result of rhs to tmpVar
        computation.append(tmpVar).append(SPACE).append(ASSIGN)
                .append(".bool").append(SPACE).append(rhsResult.getCode()).append(END_STMT);

        // add endLabel
        computation.append(endLabel).append(":").append(NEW_LINE);

        // add tmpVar to code
        code.append(tmpVar);

        return new OllirExprResult(code.toString(), computation.toString());
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

        Type type = TypeUtils.getExprType(node, table); // get the type of the variable

        String scope = (String)type.getObject("scope");

        if (scope.equals("import")){
            return new OllirExprResult(id); // return the import
        }

        String ollirType = OptUtils.toOllirType(type); // convert it to ollir type

        String var = id + ollirType; // create the code which is the name of the variable + its type

        if (scope.equals("field")){
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
        computation.append("invokespecial(").append(nt).append(", \"<init>\").V").append(END_STMT);

        // code is the temp variable
        code.append(nt);

        return new OllirExprResult(code.toString(), computation.toString());
    }

}
