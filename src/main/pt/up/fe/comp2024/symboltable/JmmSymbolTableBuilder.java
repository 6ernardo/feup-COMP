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

        var jmmSymbolTable = new JmmSymbolTable(imports, className, superClass, fields, methods, returnTypes, params, locals);

        // print table
        jmmSymbolTable.print();

        return jmmSymbolTable;
    }


    private static List<String> buildImports(JmmNode root){
        List<String> imports = new ArrayList<>();

        for (var child : root.getChildren(Kind.IMPORT_DECL)) {

            // get last import name
            // ignore warning
            @SuppressWarnings("unchecked")
            var impNames = (ArrayList<String>)child.getObject("names");


            // get last import name

            var lastImport = impNames.get(impNames.size() - 1);

            imports.add(lastImport);
        }

        return imports;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (var field : classDecl.getChildren(VAR_DECL)) {
            var varName = field.get("name");

            var varType = new Type(field.getChild(0).get("name"),
                    field.getChild(0).get("isArray").equals("true"));
            fields.add(new Symbol(varType, varName));
        }

        return fields;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for ( var method : classDecl.getChildren(METHOD_DECL)){
            var methodName = method.get("name");

            methods.add(methodName);
        }
        return methods;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");

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
                var stringType = new Type("String", true);
                stringType.putObject("isVarArgs", false);
                paramsList.add(new Symbol(stringType, paramName));
                map.put(methodName, paramsList);
                continue;
            }

            List<Symbol> paramsList = new ArrayList<>();

            for (var param : method.getChildren(PARAM)) {
                var paramName = param.get("name");                              // Get the parameter name

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


        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        List<Symbol> result = new ArrayList<>();

        var var_decls = methodDecl.getChildren(VAR_DECL);

        for (var varDecl : var_decls) {
            var type = new Type(varDecl.getJmmChild(0).get("name"),
                    varDecl.getJmmChild(0).get("isArray").equals("true"));
            type.putObject("isVarArgs", varDecl.getJmmChild(0).get("isVarArgs").equals("true"));
            var name = varDecl.get("name");
            result.add(new Symbol(type, name));
        }

        return result;
    }

}
