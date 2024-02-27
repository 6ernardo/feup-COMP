package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {

    public static JmmSymbolTable build(JmmNode root) {
        var imports = buildImports(root);

        var classDecl = root.getJmmChild(imports.size());
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        var className = classDecl.get("name");
        var superClass = classDecl.hasAttribute("superclass") ? classDecl.get("superclass") : null;
        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports, className, superClass, fields, methods, returnTypes, params, locals);
    }

    private static List<String> buildImports(JmmNode root){
        List<String> imports = new ArrayList<>();

        for (int i = 0; i < root.getNumChildren(); i++) {
            var child = root.getJmmChild(i);            // Get the child

            if (!Kind.IMPORT_DECL.check(child)) break;  // If it's not an import declaration, break

            var impName = child.get("name");            // Get the import name
            imports.add(impName);                       // Add it to the list

        }
        return imports;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (int i = 0; i < classDecl.getNumChildren(); i++) {
            var child = classDecl.getJmmChild(i);                                   // Get the child

            if (Kind.VAR_DECL.check(child)) {
                var varName = child.get("name");                                    // Get the variable name
                var varType = child.getChildren().get(0).get("name");               // Get the variable type
                fields.add(new Symbol(new Type(varType, false), varName));   // Add it to the list (Arrays not supported yet)
            }
        }

        return fields;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();

        for (int i = 0; i < classDecl.getNumChildren(); i++) {
            var child = classDecl.getJmmChild(i);                                   // Get the child

            if (Kind.METHOD_DECL.check(child)) {
                var methodName = child.get("name");                                 // Get the method name
                methods.add(methodName);                                            // Add it to the list
            }
        }

        return methods;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (int i = 0; i < classDecl.getNumChildren(); i++) {
            var child = classDecl.getJmmChild(i);                                   // Get the child


            if (Kind.METHOD_DECL.check(child)) {
                var methodName = child.get("name");                                 // Get the method name

                List<Symbol> paramsList = new ArrayList<>();                        // Create a list for the parameters

                for (int y = 0; y < child.getNumChildren(); y++) {
                    var methodChild = child.getJmmChild(y);

                    if (Kind.PARAM.check(methodChild)) {
                        var paramName = methodChild.get("name");                                    // Get the parameter name
                        var paramType = methodChild.getChildren().get(0).get("name");               // Get the parameter type
                        paramsList.add(new Symbol(new Type(paramType, false), paramName));   // Add it to the list
                    }
                }
                map.put(methodName, paramsList);                                    // Add the list to the map
            }
        }

        return map;
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
