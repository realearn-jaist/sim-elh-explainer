package io.github.xlives.util.syntaxanalyzer;

import java.util.HashSet;
import java.util.Set;

public class KRSSHandlerContextImpl extends HandlerContextImpl {

    private Set<String> superRoleSet = new HashSet<String>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Set<String> addToSuperRoleSet(String role) {
        this.superRoleSet.add(role);
        return this.superRoleSet;
    }

    public void clear() {
        super.clear();
        this.superRoleSet.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public Set<String> getSuperRoleSet() {
        return superRoleSet;
    }
}