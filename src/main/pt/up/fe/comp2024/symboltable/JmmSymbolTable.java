package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    private final String className;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;

    // Added fields
    private final List<String> imports;
    private final List<Symbol> fiels;

    public JmmSymbolTable(String className,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals,
                          List<String> imports,
                          List<Symbol> fields) {
        this.className = className;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;

        this.imports = imports;
        this.fiels = fields;
    }

    @Override
    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        throw new NotImplementedException();
    }

    @Override
    public List<Symbol> getFields() {
        return Collections.unmodifiableList(fiels);
    }

    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        if (!returnTypes.containsKey(methodSignature))
            return null;
        return returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.get(methodSignature));
    }

}
