package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
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

    boolean needsPop = false;

    private int current_stack;
    private int stack_limit;

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
        generators.put(PutFieldInstruction.class, this::generatePutFields);
        generators.put(GetFieldInstruction.class, this::generateGetFields);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
    }

    public List<Report> getReports() {
        return reports;
    }

    private void updateStack(int n){
        this.current_stack += n;
        this.stack_limit = Math.max(this.current_stack, this.stack_limit);
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        // print code
        System.out.println("Jasmin code:");
        System.out.println(code);

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
            methodSignature.append(getTypeSignature(paramType));
        }
        methodSignature.append(")");
        methodSignature.append(getTypeSignature(method.getReturnType()));

        // Calculate local limits
        int locals = method.isStaticMethod() ? 0 : 1;
        for(Descriptor var : method.getVarTable().values()){
            locals = Math.max(locals, var.getVirtualReg() + 1);
        }

        this.stack_limit = 0;
        this.current_stack = 0;

        HashMap<String,Instruction> labels = method.getLabels();
        var instruction_code = new StringBuilder();
        for (var inst : method.getInstructions()) {

            if((inst instanceof CallInstruction) && ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID
                    && (((CallInstruction) inst).getInvocationType() == CallType.invokestatic ||
                    ((CallInstruction) inst).getInvocationType() == CallType.invokevirtual ) ){
                this.needsPop = true;
            }

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            // *********************** NEEDED FOR CONTROL FLOW TESTS *******************************
            for (Map.Entry<String, Instruction> entry : labels.entrySet()) {
                if (entry.getValue() == inst) {
                    // Prepend the label to the instruction code
                    instCode = entry.getKey() + ":" + NL + instCode;
                    break;
                }
            }

            instruction_code.append(instCode);
        }

        // Append the method signature to the code
        code.append("\n.method ").append(modifier).append(static_).append(final_).append(methodName).append(methodSignature).append(NL);
        // Add limits
        code.append(TAB).append(".limit stack ").append(this.stack_limit).append(NL);
        code.append(TAB).append(".limit locals ").append(locals).append(NL);
        code.append(instruction_code);
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        if(assign.getRhs() instanceof CallInstruction){
            if (((CallInstruction) assign.getRhs()).getInvocationType() == CallType.invokevirtual || ((CallInstruction) assign.getRhs()).getInvocationType() == CallType.invokestatic){
                this.needsPop = false;
            }
        }
        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg_number = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var reg = reg_number < 4 ? "_" + reg_number : " " + reg_number;

        var command = new StringBuilder();
        switch (assign.getTypeOfAssign().getTypeOfElement()) {
            case INT32, BOOLEAN:
                if(currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement()
                        == ElementType.ARRAYREF){
                    command.append("iastore");
                }
                else {
                    command.append("istore");
                }
                break;
            case OBJECTREF, THIS, STRING, ARRAYREF:
                command.append("astore");
                break;
            default:
                command.append("error");
        }

        code.append(command).append(reg).append(NL).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement element) {
        //return "ldc " + literal.getLiteral() + NL;
        String result = "";
        String literal = element.getLiteral();
        ElementType elementType = element.getType().getTypeOfElement();
        if (elementType != ElementType.INT32 && elementType != ElementType.BOOLEAN) {
            result += "ldc " + literal;
        } else {
            int value = Integer.parseInt(literal);

            // Priority
            if (value>= -1 && value<=5) result += "iconst_";
            else if (value>= -128 && value<=127) result += "bipush ";
            else if (value>= -32768 && value<=32767) result += "sipush ";
            else result += "ldc ";

            result += (value == -1) ? "m1" : value;
        }

        this.updateStack(1);
        return result + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg_number = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var reg = reg_number < 4 ? "_" + reg_number : " " + reg_number;

        this.updateStack(1);

        switch (operand.getType().getTypeOfElement()) {
            case THIS -> {
                return "aload_0" + NL;
            }
            case STRING, ARRAYREF, OBJECTREF -> {
                return "aload" + reg + NL;
            }
            case BOOLEAN, INT32 -> {
                return "iload" + reg + NL;
            }
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement().toString());
        }
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
            case SUB -> "isub";
            case DIV -> "idiv";
            case ANDB -> "iand";
            case NOTB -> "ifeq";
            case LTH -> "if_icmplt";
            case GTE -> "if_icmpte";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var returnOperand = returnInst.getOperand();

        if (returnOperand == null) {
            return NL + "return" + NL;
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
            case CLASS, ARRAYREF, OBJECTREF: returnInstruction = "areturn"; break;
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
            case CLASS: return "L" + this.getImportedClassName(type.toString()) + ";";
            case ARRAYREF: return "[" + getTypeSignature(((ArrayType) type).getElementType());
            case OBJECTREF : return "L" + ((ClassType)type).getName() + ";";
            case STRING: return "Ljava/lang/String;";
            default: throw new NotImplementedException(type.getTypeOfElement().toString());
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

        var load1 = generators.apply(putFieldInstruction.getObject());
        var load2 = generators.apply(putFieldInstruction.getValue());
        var class_name = this.getImportedClassName(putFieldInstruction.getObject().getName());
        var putfield = "putfield " + class_name + "/" + putFieldInstruction.getField().getName() + " " + getTypeSignature(putFieldInstruction.getField().getType());

        code.append(load1).append(load2).append(putfield).append(NL).append(NL);

        return code.toString();
    }

    private String generateGetFields(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        var load = generators.apply(getFieldInstruction.getObject());
        var class_name = this.getImportedClassName(getFieldInstruction.getObject().getName());
        var getfield = "getfield " + class_name + "/" + getFieldInstruction.getField().getName() + " " + getTypeSignature(getFieldInstruction.getField().getType());

        code.append(load).append(getfield).append(NL).append(NL);
        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        var code = new StringBuilder();

        if(callInstruction.getInvocationType() == CallType.NEW){
            if(callInstruction.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                var name = callInstruction.getCaller().getType().getTypeOfElement() == ElementType.THIS ?
                        ((ClassType) callInstruction.getCaller().getType()).getName() : getImportedClassName(((ClassType) callInstruction.getCaller().getType()).getName());
                var instance = "new " + name;

                this.needsPop = true;

                return code.append(instance).append(NL).append("dup").append(NL).toString();
            }
            else { // ARRAYREF
                StringBuilder loads = new StringBuilder();
                for(Element parameter : callInstruction.getArguments()){
                    loads.append(generators.apply(parameter));
                }

                code.append(loads).append("newarray int").append(NL);
            }
        }
        else if(callInstruction.getInvocationType() == CallType.invokespecial){
            var load = generators.apply(callInstruction.getCaller());

            StringBuilder args = new StringBuilder();
            for(Element parameter : callInstruction.getArguments()){
                args.append(getTypeSignature(parameter.getType()));
            }

            var name = callInstruction.getCaller().getType().getTypeOfElement() == ElementType.THIS ?
                    ((ClassType) callInstruction.getCaller().getType()).getName() : getImportedClassName(((ClassType) callInstruction.getCaller().getType()).getName());

            var invoke = callInstruction.getInvocationType().name() + " " + name + "/<init>(" + args + ")" + getTypeSignature(callInstruction.getReturnType());
            code.append(load).append(NL).append(invoke).append(NL);
        }
        else if(callInstruction.getInvocationType() == CallType.invokevirtual){
            var load = generators.apply(callInstruction.getCaller());

            StringBuilder loads = new StringBuilder();
            for(Element parameter : callInstruction.getArguments()){
                loads.append(generators.apply(parameter)).append(NL);
            }

            StringBuilder args = new StringBuilder();
            for(Element parameter : callInstruction.getArguments()){
                args.append(getTypeSignature(parameter.getType()));
            }

            var name = this.getImportedClassName(((ClassType) callInstruction.getCaller().getType()).getName()) + "/" + ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");
            var invoke = callInstruction.getInvocationType().name() + " " + name + "(" + args + ")" + getTypeSignature(callInstruction.getReturnType());
            code.append(load).append(NL).append(loads).append(invoke).append(NL);

        }
        else if(callInstruction.getInvocationType() == CallType.invokestatic) {

            StringBuilder loads = new StringBuilder();
            for(Element parameter : callInstruction.getArguments()){
                loads.append(generators.apply(parameter));
            }

            StringBuilder args = new StringBuilder();
            for(Element parameter : callInstruction.getArguments()){
                args.append(getTypeSignature(parameter.getType()));
            }

            var name = callInstruction.getCaller().getType().getTypeOfElement() == ElementType.THIS ?
                    ((ClassType) callInstruction.getCaller().getType()).getName() : this.getImportedClassName(((Operand) callInstruction.getCaller()).getName())
                    + "/" + ((LiteralElement) callInstruction.getMethodName()).getLiteral().replace("\"", "");
            var invoke = callInstruction.getInvocationType().name() + " " + name + "(" + args + ")" + getTypeSignature(callInstruction.getReturnType());
            code.append(loads).append(invoke).append(NL);

        }
        else if(callInstruction.getInvocationType() == CallType.arraylength) {
            var aux = generators.apply(callInstruction.getCaller());
            code.append(aux).append("arraylength").append(NL);
        }

        if (this.needsPop){
            code.append("pop").append(NL);
            this.needsPop = false;
        }

        return code.toString();
    }

    private String getImportedClassName(String name){
        if(name.equals("this")){
            return ollirResult.getOllirClass().getClassName();
        }

        for(String imported : ollirResult.getOllirClass().getImports()){
            if(imported.endsWith("." + name)) {
                return imported.replace(".", "/");
            }
        }

        return name;
    }

    private String generateOpCond(OpCondInstruction inst) {
        var code = new StringBuilder();

        var label = inst.getLabel();
        var cond = inst.getCondition();

        // TODO
        // determine what branch instruction to use out of
        // if_acmpeq, if_acmpne, if_icmpeq, if_icmpge, if_icmpgt, if_icmple, if_icmplt, if_icmpne

        // for now assume we are doing "<" between integers
        var lhs = cond.getOperands().get(0);
        var rhs = cond.getOperands().get(1);

        code.append(generators.apply(lhs));
        code.append(generators.apply(rhs));

        code.append("if_icmplt ").append(label).append(NL);
        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInstruction){
        // generate code like: "iflt label"
        // add the condition to the stack

        var condition = singleOpCondInstruction.getCondition();
        var code = generators.apply(condition);

        StringBuilder res = new StringBuilder();

        res.append(code);
        res.append("ifne ").append(singleOpCondInstruction.getLabel()).append(NL);

        return res.toString();

    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        return "goto " + gotoInstruction.getLabel() + NL;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();
        var load = generators.apply(unaryOpInstruction.getOperand());

        if(unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB){
            code.append(load).append(load).append("ixor").append(NL);
        }

        return code.toString();
    }

}
