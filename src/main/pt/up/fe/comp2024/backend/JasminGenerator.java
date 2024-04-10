package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(Field.class, this::generateFields);
        generators.put(PutFieldInstruction.class, this::generatePutFields); // to implement
        generators.put(GetFieldInstruction.class, this::generateGetFields); // to implement
        generators.put(CallInstruction.class, this::generateCall);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        var classAccess = ollirResult.getOllirClass().getClassAccessModifier() != AccessModifier.DEFAULT ?
                ollirResult.getOllirClass().getClassAccessModifier().name().toLowerCase() + " " : "";
        code.append(".class ").append(classAccess).append(className).append(NL);

        var superClass = ollirResult.getOllirClass().getSuperClass();
        var superName = (superClass != null && !superClass.isEmpty()) ? superClass : "java/lang/Object";

        code.append(".super ").append(superName).append(NL).append(NL);

        // generate class fields
        code.append(";fields").append(NL);
        for (var field: classUnit.getFields()) {
            code.append(generators.apply(field));
        }
        code.append(NL);

        var defaultConstructor = new StringBuilder();
        defaultConstructor.append("""
                ;default constructor
                .method public <init>()V
                    aload_0
                """);
        defaultConstructor.append("    invokespecial ").append(superName).append("/<init>()V").append(NL);
        defaultConstructor.append("""
                    return
                .end method
                """);
        code.append(defaultConstructor.toString());

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        String static_ = method.isStaticMethod() ? "static " : "";
        String final_ = method.isFinalMethod() ? "final " : "";

        // Generate the method signature
        StringBuilder methodSignature = new StringBuilder();
        methodSignature.append("(");
        for (Element parameter : method.getParams()){
            var paramType = parameter.getType();
            var paramTypeSignature = getTypeSignature(paramType);
            methodSignature.append(getTypeSignature(paramType));
        }
        methodSignature.append(")");
        methodSignature.append(getTypeSignature(method.getReturnType()));

        // Append the method signature to the code
        code.append("\n.method ").append(modifier).append(static_).append(final_).append(methodName).append(methodSignature).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("istore ").append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload " + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var returnOperand = returnInst.getOperand();

        if (returnOperand == null) {
            return "return" + NL;
        }
        // Generate code for the return value
        code.append(generators.apply(returnOperand));

        // Determine the return type and generate the appropriate Jasmin instruction
        Type returnType = returnInst.getOperand().getType();
        String returnInstruction;
        switch (returnType.getTypeOfElement()) {
            case INT32: returnInstruction = "ireturn"; break;
            case BOOLEAN: returnInstruction = "ireturn"; break; // Booleans are represented as integers in Jasmin
            case VOID: returnInstruction = "return"; break;
            case CLASS, ARRAYREF: returnInstruction = "areturn"; break;
            default: throw new NotImplementedException(returnType.getTypeOfElement().toString());
        }

        code.append(returnInstruction).append(NL);

        return code.toString();
    }

    private String getTypeSignature(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32: return "I";
            case BOOLEAN: return "Z";
            case VOID: return "V";
            case CLASS: return "L" + type.toString() + ";";
            case ARRAYREF: return "[" + getArrayType((ArrayType) type) + ";";
            default: throw new NotImplementedException(type.getTypeOfElement().toString());
        }
    }

    private String getArrayType(ArrayType type) {
        switch (type.getElementType().getTypeOfElement()) {
            case STRING: return "Ljava/lang/String";
            // not done
            default: return type.getElementType().getTypeOfElement().toString();
        }

    }

    private String generateFields(Field field) {
        var code = new StringBuilder();

        var access = field.getFieldAccessModifier() != AccessModifier.DEFAULT ? field.getFieldAccessModifier().name().toLowerCase() + " " : "";
        var static_ = field.isStaticField() ? "static " : "";
        var final_ = field.isFinalField() ? "final" : "";
        var name = field.getFieldName() + " ";
        var descriptor = getTypeSignature(field.getFieldType());
        var value = field.isInitialized() ? " = " + field.getInitialValue() : "";

        code.append(".field ").append(access).append(static_).append(final_).append(name).append(descriptor).append(value).append(NL);

        return code.toString();
    }

    private String generatePutFields(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        code.append("aload_0").append(NL);
        var bipush = "bipush " + ((LiteralElement) putFieldInstruction.getValue()).getLiteral();
        var class_name = putFieldInstruction.getObject().getName().equals("this") ? ollirResult.getOllirClass().getClassName() : putFieldInstruction.getObject().getName();
        var putfield = "putfield " + class_name + "/" + putFieldInstruction.getField().getName() + " " + getTypeSignature(putFieldInstruction.getField().getType());

        code.append(bipush).append(NL).append(putfield).append(NL).append(NL);

        return code.toString();
    }

    private String generateGetFields(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        code.append("aload_0").append(NL);
        var class_name = getFieldInstruction.getObject().getName().equals("this") ? ollirResult.getOllirClass().getClassName() : getFieldInstruction.getObject().getName();
        var getfield = "getfield " + class_name + "/" + getFieldInstruction.getField().getName() + " " + getTypeSignature(getFieldInstruction.getField().getType());

        code.append(getfield).append(NL).append(NL);
        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
//        var code = new StringBuilder();
//
//        //var instance_name = callInstruction.getCaller().toString();
//        //var invocation = "invocation:" + callInstruction.getInvocationType().name() + " " + instance_name;
//
//        var invocation = "inv:";
//
//        code.append(invocation);
//
//        return code.toString();

        return "";
    }
}
