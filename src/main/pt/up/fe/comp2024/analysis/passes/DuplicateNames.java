package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

public class DuplicateNames extends AnalysisVisitor {
    private String currentMethod;
    private List<String> importedClasses;
    private List<String> importPaths;
    private List<String> fields;
    private List<String> methods;
    private List<String> parameters;
    private List<String> localVariables;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.PARAM,this::visitParam);
        addVisit(Kind.PROGRAM, this::initializeLists);
    }

    private Void initializeLists(JmmNode jmmNode, SymbolTable symbolTable) {
        this.currentMethod = null;
        this.importedClasses = new ArrayList<>();
        this.importPaths = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.localVariables = new ArrayList<>();
        return null;
    }

    private Void visitParam(JmmNode param, SymbolTable table) {
        var name = param.get("name");

        if (parameters.contains(name)){
            var message = "Duplicate parameter name: " + name + " in function " + currentMethod ;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(param),
                    NodeUtils.getColumn(param),
                    message,
                    null)
            );
        }else{
            parameters.add(name);
        }
        return null;
    }

    private Void visitVarDecl(JmmNode node, SymbolTable table) {
        var name = node.get("name");
        if(currentMethod == null){ // then its a field
            if (fields.contains(name)) {
                // throw error
                var message = "Duplicate field name: " + name;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null)
                );
            }else {
                fields.add(name);
            }
        }else{ // local variable
            if(localVariables.contains(name)){
                var message = "Duplicate local variable name: " + name + " in function " + currentMethod ;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null)
                );
            }else{
                localVariables.add(name);
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        localVariables.clear();
        parameters.clear();
        // check if the method is already declared
        if (methods.contains(currentMethod)) {
            // throw error
            var message = "Duplicate method name: " + currentMethod;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }else {
            methods.add(currentMethod);
        }
        return null;
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        // get class and path of the import

        var path = new StringBuilder();
        var names = importDecl.getChildren();
        for (int i = 0 ; i< names.size(); i++){
            if (i != 0){
                path.append(".");
            }
            var packageNode = names.get(i);
            var name = packageNode.get("name");
            path.append(name);
        }
        var className = names.get(names.size()-1).get("name");
        var pathString = path.toString();

        // check if the class is already imported
        if (importedClasses.contains(className) || importPaths.contains(pathString)){

            // create error report
            var message = "Duplicate import name: " + className;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importDecl),
                    NodeUtils.getColumn(importDecl),
                    message,
                    null)
            );

        }else {
            importedClasses.add(className);
            importPaths.add(pathString);
        }
        return null;
    }

}
