package pt.up.fe.comp.cp2;

import org.junit.Test;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class OllirTest {


    @Test
    public void compileBasic() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileBasic.jmm", this::compileBasic);
    }

    @Test
    public void compileArithmetic() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileArithmetic.jmm", this::compileArithmetic);
    }

    @Test
    public void compileMethodInvocation() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileMethodInvocation.jmm",
                this::compileMethodInvocation);
    }

    @Test
    public void compileAssignment() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileAssignment.jmm", this::compileAssignment);
    }

    // added these tests
    @Test
    public void compileMisc(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileMisc.jmm", this::compileMisc);
    }

    @Test
    public void compileIfElse(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileIfElse.jmm", this::compileIfElse);
    }

    @Test
    public void compileWhile(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileWhile.jmm", this::compileWhile);
    }

    @Test
    public void compileNewArray(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileNewArray.jmm", this::compileNewArray);
    }

    @Test
    public void compileArrayLength() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileArrayLength.jmm", this::compileArrayLength);
    }

    @Test
    public void compileArrayAccessAndAllocation() {
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileArrayAccessAndAllocation.jmm",
                this::compileArrayAccessAndAllocation);
    }

    @Test
    public void compileVarArgs(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileVarArgs.jmm", this::compileVarArgs);
    }

    @Test
    public void compileArrayInit(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileArrayInit.jmm", this::compileArrayInit);
    }

    @Test
    public void compileEverything(){
        testJmmCompilation("pt/up/fe/comp/cp2/ollir/CompileEverything.jmm", this::compileEverything);
    }

    public static void testJmmCompilation(String resource, Consumer<ClassUnit> ollirTester, String executionOutput) {

        // If AstToJasmin pipeline, generate Jasmin
        if (TestUtils.hasAstToJasminClass()) {

            var result = TestUtils.backend(SpecsIo.getResource(resource));

            var testName = new File(resource).getName();
            System.out.println(testName + ":\n" + result.getJasminCode());
            var runOutput = result.runWithFullOutput();
            assertEquals("Error while running compiled Jasmin: " + runOutput.getOutput(), 0,
                    runOutput.getReturnValue());
            System.out.println("\n Result: " + runOutput.getOutput());

            if (executionOutput != null) {
                assertEquals(executionOutput, runOutput.getOutput());
            }

            return;
        }

        var result = TestUtils.optimize(SpecsIo.getResource(resource));
        var testName = new File(resource).getName();


        System.out.println(testName + ":\n" + result.getOllirCode());
        if (ollirTester != null) {
            ollirTester.accept(result.getOllirClass());
        }
    }

    public static void testJmmCompilation(String resource, Consumer<ClassUnit> ollirTester) {
        testJmmCompilation(resource, ollirTester, null);
    }

    public void compileBasic(ClassUnit classUnit) {
        // Test name of the class and super
        assertEquals("Class name not what was expected", "CompileBasic", classUnit.getClassName());
        assertEquals("Super class name not what was expected", "Quicksort", classUnit.getSuperClass());

        // Test fields
        assertEquals("Class should have two fields", 2, classUnit.getNumFields());
        var fieldNames = new HashSet<>(Arrays.asList("intField", "boolField"));
        assertThat(fieldNames, hasItem(classUnit.getField(0).getFieldName()));
        assertThat(fieldNames, hasItem(classUnit.getField(1).getFieldName()));

        // Test method 1
        Method method1 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method1"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method1", method1);

        var retInst1 = method1.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method1", retInst1.isPresent());

        // Test method 2
        Method method2 = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals("method2"))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method2'", method2);

        var retInst2 = method2.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method2", retInst2.isPresent());
    }

    public void compileArithmetic(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileArithmetic", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var binOpInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(instr -> (AssignInstruction) instr)
                .filter(assign -> assign.getRhs() instanceof BinaryOpInstruction)
                .findFirst();

        assertTrue("Could not find a binary op instruction in method " + methodName, binOpInst.isPresent());

        var retInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof ReturnInstruction)
                .findFirst();
        assertTrue("Could not find a return instruction in method " + methodName, retInst.isPresent());

    }

    public void compileMethodInvocation(ClassUnit classUnit) {

        // Test name of the class
        assertEquals("Class name not what was expected", "CompileMethodInvocation", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var callInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof CallInstruction)
                .map(CallInstruction.class::cast)
                .findFirst();
        assertTrue("Could not find a call instruction in method " + methodName, callInst.isPresent());

        assertEquals("Invocation type not what was expected", CallType.invokestatic,
                callInst.get().getInvocationType());
    }

    public void compileAssignment(ClassUnit classUnit) {

        // Test name of the class
        assertEquals("Class name not what was expected", "CompileAssignment", classUnit.getClassName());

        // Test foo
        var methodName = "foo";
        Method methodFoo = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodFoo);

        var assignInst = methodFoo.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction)
                .map(AssignInstruction.class::cast)
                .findFirst();
        assertTrue("Could not find an assign instruction in method " + methodName, assignInst.isPresent());

        assertEquals("Assignment does not have the expected type", ElementType.INT32,
                assignInst.get().getTypeOfAssign().getTypeOfElement());
    }


    // Adding Tests
    public void compileMisc(ClassUnit classUnit){
        // For testing purposes
    }

    public void compileIfElse(ClassUnit classUnit) {
        // Test name of the class
        assertEquals("Class name not what was expected", "CompileIfElse", classUnit.getClassName());

        // Test main
        var methodName = "main";
        Method methodMain = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodMain);

        // check for an instance of a BRANCH instruction

        var branchInst = methodMain.getInstructions().stream()
                .filter(inst -> inst instanceof SingleOpCondInstruction)
                .map(SingleOpCondInstruction.class::cast)
                .toList();

        assertEquals("Wrong number of Branch Instructions in method " + methodName,
                1, branchInst.size());

        // check for an instance of a GOTO instruction

        var gotoInst = methodMain.getInstructions().stream()
                .filter(inst -> inst instanceof GotoInstruction)
                .map(GotoInstruction.class::cast)
                .toList();

        assertEquals("Wrong number of Goto Instructions in method " + methodName,
        1, gotoInst.size());

        // check number of labels

        var labels = methodMain.getLabels().size();
        assertEquals("Wrong number of labels in method " + methodName,
                2, labels);
    }

    public void compileWhile(ClassUnit classUnit){
        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileWhile", classUnit.getClassName());

        // Test main
        var methodName = "main";
        Method methodMain = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodMain);

        // check for an instance of a BRANCH instruction

        var branchInst = methodMain.getInstructions().stream()
                .filter(inst -> inst instanceof SingleOpCondInstruction)
                .map(SingleOpCondInstruction.class::cast)
                .toList();

        assertEquals("Wrong number of Branch Instructions in method " + methodName,
                1, branchInst.size());

        // check for an instance of a GOTO instruction

        var gotoInst = methodMain.getInstructions().stream()
                .filter(inst -> inst instanceof GotoInstruction)
                .map(GotoInstruction.class::cast)
                .toList();

        assertEquals("Wrong number of Goto Instructions in method " + methodName,
                1, gotoInst.size());

        // check number of labels

        var labels = methodMain.getLabels().size();
        assertEquals("Wrong number of labels in method " + methodName,
                2, labels);


    }

    public void compileNewArray(ClassUnit classUnit) {
        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileNewArray", classUnit.getClassName());

        // Test main
        var methodName = "main";
        Method methodMain = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodMain);

        // check for an instance of a new array expression

        var newArrayInst = methodMain.getInstructions().stream()
                .filter(inst -> inst instanceof AssignInstruction &&
                        ((AssignInstruction) inst).getRhs() instanceof CallInstruction &&
                        ((CallInstruction) ((AssignInstruction) inst).getRhs()).getReturnType() instanceof ArrayType
                ).findFirst();

        assertTrue("Could not find a new array instruction in method " + methodName, newArrayInst.isPresent());

    }

    public void compileArrayLength(ClassUnit classUnit){

        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileArrayLength", classUnit.getClassName());

        // Test main
        var methodName = "main";
        Method methodMain = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodMain);

        // TODO: check for an instance of an array length expression

    }

    public void compileArrayAccessAndAllocation(ClassUnit classUnit){
        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileArrayAccessAndAllocation", classUnit.getClassName());

        // Test main
        var methodName = "main";
        Method methodMain = classUnit.getMethods().stream()
                .filter(method -> method.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertNotNull("Could not find method " + methodName, methodMain);

        // TODO: check for an instance of an array access expression
    }

    public void compileVarArgs(ClassUnit classUnit){
        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileVarArgs", classUnit.getClassName());

        // TODO: check for an instance of a call instruction with varargs
    }


    public void compileArrayInit(ClassUnit classUnit){
        // Test name of the Class
        assertEquals("Class name not what was expected", "CompileArrayInit", classUnit.getClassName());

        // TODO: check for an instance of an array initialization expression
    }

    public void compileEverything(ClassUnit classUnit){

    }

}
