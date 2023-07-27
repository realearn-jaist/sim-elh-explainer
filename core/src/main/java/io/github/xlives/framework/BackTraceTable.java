package io.github.xlives.framework;

import io.github.xlives.framework.descriptiontree.Tree;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BackTraceTable {
    private static Integer index = 0;

    private Map<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>> backTraceTable = new LinkedHashMap<>();

    private String[] cnPair = new String[2];

    // method to input values for the innermost list of tree nodes
    public void inputTreeNodeValue(Tree<Set<String>> node, BigDecimal values, int order) {

        Map<Tree<Set<String>>, BigDecimal> treeNodeMap = new HashMap<>();
        Map<String, Map<Tree<Set<String>>, BigDecimal>> innerMap = new HashMap<>();

        treeNodeMap.put(node, values);

        if (order == 1){
            innerMap.put(cnPair[0], treeNodeMap);
        } else {
            innerMap.put(cnPair[1], treeNodeMap);
        }

        Map<Integer, String[]> outermostMap = setKeyMap(index, cnPair);

        if (!backTraceTable.containsKey(outermostMap)) {
            backTraceTable.put(outermostMap, innerMap);
            index++;
        }
    }

    // method to input a key for the outermost HashMap
    public void inputConceptName(String concept1, String concept2) {
        cnPair[0] = concept1;
        cnPair[1] = concept2;
    }

    public Map<Integer, String[]> setKeyMap(Integer index, String[] cnPair) {
        Map<Integer, String[]> outermostMap = new HashMap<>();
        outermostMap.put(index, cnPair);
        return outermostMap;
    }

    public Map<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>> getBackTraceTable() {
        return backTraceTable;
    }

}
