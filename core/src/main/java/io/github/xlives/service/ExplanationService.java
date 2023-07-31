package io.github.xlives.service;

import io.github.xlives.framework.BackTraceTable;
import io.github.xlives.framework.descriptiontree.Tree;
import io.github.xlives.framework.descriptiontree.TreeNode;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ExplanationService {

    private static StringBuilder explanation = new StringBuilder();

    private File output_file = new File("/Users/rchn/Desktop/refactor/sim-elh-explainer/explanation/explanation");
    private File backtrace_file = new File("/Users/rchn/Desktop/refactor/sim-elh-explainer/explanation/backtracetable");

    public void explainSimilarity(BackTraceTable backTraceTable) throws IOException {

        if (output_file.exists() && backtrace_file.exists()) {
            output_file.delete();
            backtrace_file.delete();
        }

        BigDecimal degree = null;

        String conceptName1 = "";
        String conceptName2 = "";

        StringBuilder res = new StringBuilder();

        List<String[]> priList = new ArrayList<>();
        List<List<String>> exiList = new ArrayList<>();

        // iterate through each value in backTraceTable
        Deque<Map.Entry<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>>> lastTwoEntries = new LinkedList<>();

        for (Map.Entry<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>> backtrace : backTraceTable.getBackTraceTable().entrySet()) {

            Map<Integer, String[]> keyMap = backtrace.getKey();

            for (int i = 0; i < keyMap.size(); i++) {
                String[] arrayValue = keyMap.get(i);
                if (arrayValue != null) {
                    for (int j = 0; j < arrayValue.length; j++) {
                        if (j == 0) {
                            conceptName1 = arrayValue[0];
                        } else {
                            conceptName2 = arrayValue[1];
                        }
                    }
                }
            }

            lastTwoEntries.add(backtrace);
            if (lastTwoEntries.size() > 2) {
                lastTwoEntries.removeFirst(); // keep only the last two
            }
        }

        for (Map.Entry<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>> backtrace : lastTwoEntries) {

            Map<String, Map<Tree<Set<String>>, BigDecimal>> valueMap = backtrace.getValue();

            // retrieve primitives and degree along with its concept name
            for (Map.Entry<String, Map<Tree<Set<String>>, BigDecimal>> entry : valueMap.entrySet()) {
                String[] priArr = new String[1];
                List<String> exiEach = new ArrayList<>();

                String key = entry.getKey();
                String exi;

                for (Map.Entry<Tree<Set<String>>, BigDecimal> child : entry.getValue().entrySet()) {

                    // existential
                    for (Map.Entry<Integer, TreeNode<Set<String>>> tree : child.getKey().getNodes().entrySet()) {
                        exi = tree.getValue().getEdgeToParent();

                        if (exi != null) {
                            exiEach.add(exi);
                        }
                    }

                    degree = child.getValue();

                    priArr[0] = child.getKey().getNodes().get(0).toString();
                    removeUnwantedChar(priArr);
                    priList.add(priArr);
                    exiList.add(exiEach);
                }

                res.append("\t").append(key).append(" = ").append(Arrays.toString(priArr));

                if(!exiEach.isEmpty()) {
                    res.append(", ").append(exiEach);
                }

                res.append("\n");
            }
        }

        // explanation details
        Set<String> matchingCon = findMatchingConcept(priList);
        Set<String> matchingRole = findMatchingRole(exiList);

        if (matchingCon.size() == 0) {
            matchingCon.add("nothing");
        }

        explanation.append("The similarity between ").append(conceptName1).append(" and ").append(conceptName2)
                .append(" is ").append(degree.setScale(5, BigDecimal.ROUND_HALF_UP));
        explanation.append(" because they have ").append(matchingCon).append(" in common.");

        if (!matchingRole.isEmpty()) {
            explanation.append(" Moreover, both of them also ").append(matchingRole).append(".");
        }

        explanation.append("\n").append(res);

        FileUtils.writeStringToFile(backtrace_file, backTraceTable.getBackTraceTable().toString(), true);
        FileUtils.writeStringToFile(output_file, explanation.toString(), false);
    }

    private void removeUnwantedChar(String[] wordsArray) {
        for (int i = 0; i < wordsArray.length; i++) {
            wordsArray[i] = wordsArray[i].replaceAll("'", "");
        }
    }

    private static Set<String> findMatchingConcept(List<String[]> wordsList) {
        Set<String> matchingWords = new HashSet<>();

        if (!wordsList.isEmpty()) {
            String[] concept1 = wordsList.get(0);
            String[] concept2 = wordsList.get(1);

            int c1_length = concept1.length;
            int c2_length = concept2.length;

            for (int i = 0; i < c1_length; i++) {
                String[] conName1 = concept1[i].split("\\s+");

                for (int j = 0; j < c2_length; j++) {
                    String[] conName2 = concept2[i].split("\\s+");

                    for (String cn1 : conName1) {
                        for (String cn2 : conName2) {
                            if (cn1.equals(cn2)) {
                                matchingWords.add(cn1);
                            }
                        }
                    }
                }
            }
        }

        return matchingWords;
    }

    public Set<String> findMatchingRole(List<List<String>> listOfLists) {
        Set<String> matchingStrings = new HashSet<>();

        List<String> firstSet = listOfLists.get(0);
        List<String> secondSet = listOfLists.get(1);

        if (!firstSet.isEmpty()) {
            for (String list : firstSet) {
                for (String str : secondSet) {
                    if (str.equals(list) && !str.equals("** DO NOT HAVE ANY ROLES **")) {
                        matchingStrings.add(str);
                    }
                }
            }
        }

        return matchingStrings;
    }
}