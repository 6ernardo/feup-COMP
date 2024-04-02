package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

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

        for (var child : root.getChildren(Kind.IMPORT_DECL)){

            // get last import name
            var impNames = child.getChildren();

            var lastNameNode = impNames.get(impNames.size() - 1);

            var lastName = lastNameNode.get("name");
            // see if there is already an import with the same name
            if (imports.contains(lastName)) {
                throw new RuntimeException("More than one import with the same name: " + lastName);
            }
            imports.add(lastName);                     // Add it to the list

        }
        return imports;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (var field : classDecl.getChildren(VAR_DECL)) {
            var varName = field.get("name");// Get the variable name
            // see if it already exists
            if (fields.stream().anyMatch(symbol -> symbol.getName().equals(varName))) {
                throw new RuntimeException("Field with the same name already exists: " + varName);
            }
            var varTypeName = field.getChild(0).get("name");               // Get the variable type
            var isVarTypeArray = field.getChild(0).get("isArray").equals("true");

            var isVarTypeVarArgs = field.getChild(0).get("isVarArgs").equals("true");

            if (isVarTypeVarArgs) {
                throw new RuntimeException("Field cannot be a varargs: " + varName);
            }

            var varType = new Type(varTypeName, isVarTypeArray);                // Create a new type

            fields.add(new Symbol(varType, varName));   // Add it to the list
        }

        return fields;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for ( var method : classDecl.getChildren(METHOD_DECL)){
            var methodName = method.get("name");
            // see if it already exists
            if (methods.contains(methodName)) {
                throw new RuntimeException("More than one method with the same name");
            }
            methods.add(methodName);
        }
        return methods;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");

            if (method.getJmmChild(0).get("isVarArgs").equals("true")) {
                throw new RuntimeException("Return type cannot be a varargs");
            }

            var returnType = new Type(method.getJmmChild(0).get("name"),
                    method.getJmmChild(0).get("isArray").equals("true"));
            map.put(name, returnType);
        }

        return map;
    }


    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var methodName = method.get("name");

            if (methodName.equals("main")){
                var paramName = method.get("paramName");
                List<Symbol> paramsList = new ArrayList<>();
                paramsList.add(new Symbol(new Type("String", true), paramName));
                map.put(methodName, paramsList);
                continue;
            }

            List<Symbol> paramsList = new ArrayList<>();

            for (var param : method.getChildren(PARAM)) {
                var paramName = param.get("name");                              // Get the parameter name

                if (paramsList.stream().anyMatch(symbol -> symbol.getName().equals(paramName))) {
                    throw new RuntimeException("Parameter with the same name already exists: " + paramName);
                }

                var paramTypeName = param.getJmmChild(0).get("name");
                var isParamTypeArray = param.getJmmChild(0).get("isArray").equals("true");
                var isParamTypeVarArgs = param.getJmmChild(0).get("isVarArgs").equals("true");


                var type = new Type(paramTypeName, isParamTypeArray);
                type.putObject("isVarArgs", isParamTypeVarArgs);

                paramsList.add(new Symbol(type, paramName));
            }
            map.put(methodName, paramsList);
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

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(
                        new Type(varDecl.getJmmChild(0).get("name"),
                                varDecl.getJmmChild(0).get("isArray").equals("true"))
                        , varDecl.get("name")))
                .toList();
    }

}
