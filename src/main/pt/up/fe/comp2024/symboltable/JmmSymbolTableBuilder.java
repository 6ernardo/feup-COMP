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

        var classDecl = root.getJmmChild(root.getNumChildren() - 1); // was 0 changed to i

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        String superClass = classDecl.hasAttribute("superclass") ? classDecl.get("superclass") : null;

        var fields = buildFields(classDecl); // added this line
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superClass, methods, returnTypes, params, locals, imports, fields);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded
        Map<String, Type> map = new HashMap<>();

        var methods = classDecl.getChildren(METHOD_DECL);

        for (var method : methods) {
            var name = method.get("name");
            var returnType = new Type(method.getJmmChild(0).get("name"),
                    method.getJmmChild(0).get("isArray").equals("true"));
            map.put(name, returnType);
        }

        return map;
    }

    private static List<String> buildImports(JmmNode root){
        int i= 0;
        List<String> imports = new ArrayList<>();

        while(true){
            var temp = root.getJmmChild(i);
            if (!Kind.IMPORT_DECL.check(temp)) break;
            i++;
            var impname = temp.get("name");
            imports.add(impname);
        }
        return imports;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol( new Type(varDecl.getJmmChild(0).get("name"),
                        varDecl.getJmmChild(0).get("isArray").equals("true")
                ), varDecl.get("name"))).toList();
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var methods = classDecl.getChildren(METHOD_DECL);
                //.forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));
        for (var method : methods) {
            var name = method.get("name");
            List<Symbol> params = new ArrayList<>();
            for (var param : method.getChildren(PARAM)) {
                var paramName = param.get("name");
                var paramType = new Type(param.getChild(0).get("name"),
                        param.getChild(0).get("isArray").equals("true"));
                params.add(new Symbol(paramType, param.get("name")));
            }
            map.put(method.get("name"), params);
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

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
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
