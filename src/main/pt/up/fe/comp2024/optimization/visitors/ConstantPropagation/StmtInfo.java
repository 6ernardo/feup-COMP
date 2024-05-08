package pt.up.fe.comp2024.optimization.visitors.ConstantPropagation;

import java.util.*;

public class StmtInfo {
    private Set<String> changed;
    private Set<String> used;

    public StmtInfo(Set<String> changed, Set<String> used) {
        this.changed = changed;
        this.used = used;
    }

    public StmtInfo() {
        this.changed = new HashSet<>();
        this.used = new HashSet<>();
    }

    public StmtInfo(StmtInfo stmtInfo) {
        this.changed = new HashSet<>(stmtInfo.changed);
        this.used = new HashSet<>(stmtInfo.used);
    }

    public Set<String> getChanged() {
        return changed;
    }
    public Set<String> getUsed() {
        return used;
    }
    public void setChanged(Set<String> changed) {
        this.changed = changed;
    }
    public void setUsed(Set<String> used) {
        this.used = used;
    }
    public void addChanged(String changed) {
        this.changed.add(changed);
    }
    public void addUsed(String used) {
        this.used.add(used);
    }
    public void addChanged(Collection<String> changed) {
        this.changed.addAll(changed);
    }
    public void addUsed(Collection<String> used) {
        this.used.addAll(used);
    }
}
