package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.optimization.visitors.ConstantFolding;
import pt.up.fe.comp2024.optimization.visitors.ConstantPropagation.ConstantPropagation;
import pt.up.fe.comp2024.optimization.visitors.ConstantPropagation.VariableInfoGeneratorVisitor;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        // print result
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        var registersCountString = ollirResult.getConfig().get("registerAllocation");
        if (registersCountString == null){
            return ollirResult;
        }
        int regCount = Integer.parseInt(registersCountString);

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (semanticsResult.getConfig().get("optimize") == null) {
            return semanticsResult;
        }

        for(int i = 0; i < 100; i++){

            // add info to AST
            var vis = new VariableInfoGeneratorVisitor();
            vis.visit(semanticsResult.getRootNode());

            var constantPropagation = new ConstantPropagation();
            constantPropagation.visit(semanticsResult.getRootNode());

            var constantFold = new ConstantFolding();
            constantFold.visit(semanticsResult.getRootNode());

            if(!constantPropagation.changed && !constantFold.changed){
                break;
            }
        }

        System.out.println("Optimized AST");
        System.out.println(semanticsResult.getRootNode().toTree());

        return semanticsResult;
    }
}
