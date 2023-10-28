package io.github.xlives.framework;
import io.github.xlives.framework.descriptiontree.TreeBuilder;
import io.github.xlives.framework.reasoner.*;
import io.github.xlives.framework.unfolding.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.xlives.framework.descriptiontree.Tree;
import io.github.xlives.framework.descriptiontree.TreeNode;
import io.github.xlives.service.SimilarityService;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.github.xlives.enumeration.TypeConstant.DYNAMIC_SIM;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SimilarityService.class, SuperRoleUnfolderManchesterSyntax.class,
        TopDownSimReasonerImpl.class, TopDownSimPiReasonerImpl.class,
        DynamicProgrammingSimReasonerImpl.class, DynamicProgrammingSimPiReasonerImpl.class,
        ConceptDefinitionUnfolderManchesterSyntax.class, ConceptDefinitionUnfolderKRSSSyntax.class,
        SuperRoleUnfolderKRSSSyntax.class,
        TreeBuilder.class, OWLServiceContext.class, KRSSServiceContext.class,
        PreferenceProfile.class, BackTraceTable.class
})
public class BackTraceTableTests {

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private OWLServiceContext owlServiceContext;

    @Autowired
    private BackTraceTable backTraceTable;

    @Autowired
    private IConceptUnfolder conceptDefinitionUnfolderManchesterSyntax;


    private final String owlFilePath = "src/test/resources/family.owl";

    // runchana:2023-10-20 verify that the concepts have been set correctly
    @Test
    public void testInputConceptName() {
        String concept1 = "Female";
        String concept2 = "Male";

        backTraceTable.inputConceptName(concept1, concept2);

        assertEquals(concept1, backTraceTable.getCnPair()[0]);
        assertEquals(concept2, backTraceTable.getCnPair()[1]);
    }

    /**
     * runchana:2023-10-20 Check the list of concepts in BackTraceTable, which will be used for explanation extraction.
     */
    @Test
    public void testTreeNodeMapBackTraceTable() throws IOException {

        owlServiceContext.init(owlFilePath);

        IConceptUnfolder conceptT = conceptDefinitionUnfolderManchesterSyntax; // OWL

        // Populate description trees of Female and Man directly
        Tree<Set<String>> tree1 = similarityService.unfoldAndConstructTree(conceptT, "Female");
        Tree<Set<String>> tree2 = similarityService.unfoldAndConstructTree(conceptT, "Man");

        Map<Integer, TreeNode<Set<String>>> tree1_concepts = tree1.getNodes();
        Map<Integer, TreeNode<Set<String>>> tree2_concepts = tree2.getNodes();

        int index = 0;

        BigDecimal result1 = similarityService.measureConceptWithType("Female", "Man", DYNAMIC_SIM, "OWL").setScale(5, BigDecimal.ROUND_HALF_UP);

        for (Map.Entry<Map<Integer, String[]>, Map<String, Map<Tree<Set<String>>, BigDecimal>>> backtrace : similarityService.backTraceTable.getBackTraceTable().entrySet()) {
            Map<String, Map<Tree<Set<String>>, BigDecimal>> valueMap = backtrace.getValue();

            for (Map.Entry<String, Map<Tree<Set<String>>, BigDecimal>> treeNode : valueMap.entrySet()) {
                for (Map.Entry<Tree<Set<String>>, BigDecimal> treeEntry : treeNode.getValue().entrySet()) {
                    Tree<Set<String>> tree = treeEntry.getKey();
                    if (index == 0) {
                        assertEquals("Female tree", tree.getLabel()); // check concept tree
                        assertTreeNodesEqual(tree.getNodes(), tree1_concepts); // check a list of its concept names
                    } else if (index == 1) {
                        assertEquals("Man tree", tree.getLabel());
                        assertTreeNodesEqual(tree.getNodes(), tree2_concepts);
                    }
                    assertEquals(treeEntry.getValue().setScale(5, BigDecimal.ROUND_HALF_UP), result1); // check homomorphism degree

                }
                index++;
            }
        }
    }

    private void assertTreeNodesEqual(Map<Integer, TreeNode<Set<String>>> tree1, Map<Integer, TreeNode<Set<String>>> tree2) {
        assertEquals(tree1.size(), tree2.size());

        for (Map.Entry<Integer, TreeNode<Set<String>>> entry1 : tree1.entrySet()) {
            int key = entry1.getKey();
            TreeNode<Set<String>> node1 = entry1.getValue();
            TreeNode<Set<String>> node2 = tree2.get(key);

            assertEquals(node1.getEdgeToParent(), node2.getEdgeToParent());
            assertEquals(node1.getData(), node2.getData());
        }
    }

}
