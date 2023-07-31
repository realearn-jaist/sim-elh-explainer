package io.github.xlives.service;

import io.github.xlives.enumeration.TypeConstant;
import io.github.xlives.exception.ErrorCode;
import io.github.xlives.exception.JSimPiException;
import io.github.xlives.framework.BackTraceTable;
import io.github.xlives.framework.descriptiontree.Tree;
import io.github.xlives.framework.descriptiontree.TreeBuilder;
import io.github.xlives.framework.reasoner.DynamicProgrammingSimPiReasonerImpl;
import io.github.xlives.framework.reasoner.DynamicProgrammingSimReasonerImpl;
import io.github.xlives.framework.reasoner.IReasoner;
import io.github.xlives.framework.reasoner.TopDownSimReasonerImpl;
import io.github.xlives.framework.unfolding.ConceptDefinitionUnfolderManchesterSyntax;
import io.github.xlives.framework.unfolding.IConceptUnfolder;
import io.github.xlives.framework.unfolding.IRoleUnfolder;
import io.github.xlives.util.MyStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SimilarityService {

    private static final BigDecimal TWO = new BigDecimal("2");

    @Resource(name="topDownSimReasonerImpl")
    private IReasoner topDownSimReasonerImpl;

    @Resource(name="topDownSimPiReasonerImpl")
    private IReasoner topDownSimPiReasonerImpl;

    @Resource(name="dynamicProgrammingSimReasonerImpl")
    private IReasoner dynamicProgrammingSimReasonerImpl;

    @Resource(name="dynamicProgrammingSimPiReasonerImpl")
    private IReasoner dynamicProgrammingSimPiReasonerImpl;

    @Resource(name="conceptDefinitionUnfolderManchesterSyntax")
    private IConceptUnfolder conceptDefinitionUnfolderManchesterSyntax;

    @Resource(name="conceptDefinitionUnfolderKRSSSyntax")
    private IConceptUnfolder conceptDefinitionUnfolderKRSSSyntax;

    @Resource(name="superRoleUnfolderManchesterSyntax")
    private IRoleUnfolder superRoleUnfolderManchesterSyntax;

    @Resource(name="superRoleUnfolderKRSSSyntax")
    private IRoleUnfolder superRoleUnfolderKRSSSyntax;

    @Autowired
    private TreeBuilder treeBuilder;

    private Map<String, Map<String, List<String>>> topDownSimExecutionMap = new HashMap<String, Map<String, List<String>>>();
    private Map<String, Map<String, List<String>>> topDownSimPiExecutionMap = new HashMap<String, Map<String, List<String>>>();
    private Map<String, Map<String, List<String>>> dynamicProgrammingSimExecutionMap = new HashMap<String, Map<String, List<String>>>();
    private Map<String, Map<String, List<String>>> dynamicProgrammingSimPiExecutionMap = new HashMap<String, Map<String, List<String>>>();

    private ExplanationService explanationService = new ExplanationService();
    private BackTraceTable backTraceTable = new BackTraceTable();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Tree<Set<String>> unfoldAndConstructTree(IConceptUnfolder iConceptUnfolder, String conceptName1) {
        String unfoldConceptName1 = iConceptUnfolder.unfoldConceptDefinitionString(conceptName1);

        if (iConceptUnfolder instanceof ConceptDefinitionUnfolderManchesterSyntax) {
            return treeBuilder.constructAccordingToManchesterSyntax(MyStringUtils.generateTreeLabel(conceptName1), unfoldConceptName1);
        }

        else {
            return treeBuilder.constructAccordingToKRSSSyntax(MyStringUtils.generateTreeLabel(conceptName1), unfoldConceptName1);
        }
    }

    private BigDecimal computeSimilarity(IReasoner iReasoner, IRoleUnfolder iRoleUnfolder, Tree<Set<String>> tree1, Tree<Set<String>> tree2) throws IOException {
        iReasoner.setRoleUnfoldingStrategy(iRoleUnfolder);

        BigDecimal forwardDistance = iReasoner.measureDirectedSimilarity(tree1, tree2);
        // Removed later
        reckonTime(iReasoner, tree1, tree2);

        BigDecimal backwardDistance = iReasoner.measureDirectedSimilarity(tree2, tree1);
        // Removed later
        reckonTime(iReasoner, tree2, tree1);

        return forwardDistance.add(backwardDistance).divide(TWO);
    }

    private void reckonTime(IReasoner iReasoner, Tree<Set<String>> tree1, Tree<Set<String>> tree2) {
        if (iReasoner instanceof DynamicProgrammingSimReasonerImpl) {
            updateMap(tree1, tree2, dynamicProgrammingSimExecutionMap, dynamicProgrammingSimReasonerImpl);
        }

        else if (iReasoner instanceof TopDownSimReasonerImpl) {
            updateMap(tree1, tree2, topDownSimExecutionMap, topDownSimReasonerImpl);
        }

        else if (iReasoner instanceof DynamicProgrammingSimPiReasonerImpl){
            updateMap(tree1, tree2, dynamicProgrammingSimPiExecutionMap, dynamicProgrammingSimPiReasonerImpl);
        }

        else {
            updateMap(tree1, tree2, topDownSimPiExecutionMap, topDownSimPiReasonerImpl);
        }
    }

    private void updateMap(Tree<Set<String>> tree1, Tree<Set<String>> tree2, Map<String, Map<String, List<String>>> dynamicProgrammingSimExecutionMap, IReasoner dynamicProgrammingSimReasonerImpl) {
        Map<String, List<String>> tmp = dynamicProgrammingSimExecutionMap.get(tree1.getLabel());
        if (tmp == null) {
            tmp = new HashMap<String, List<String>>();
        }
        tmp.put(tree2.getLabel(), dynamicProgrammingSimReasonerImpl.getExecutionTimes());
        dynamicProgrammingSimExecutionMap.put(tree1.getLabel(), tmp);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BigDecimal measureOWLConceptsWithDynamicProgrammingSim(String conceptName1, String conceptName2) throws IOException {
        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with dynamic programming Sim as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptDefinitionUnfolderManchesterSyntax, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptDefinitionUnfolderManchesterSyntax, conceptName2);

        return computeSimilarity(dynamicProgrammingSimReasonerImpl, superRoleUnfolderManchesterSyntax, tree1, tree2);
    }

    public BigDecimal measureOWLConceptsWithDynamicProgrammingSimPi(String conceptName1, String conceptName2) throws IOException {
        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with dynamic programming SimPi as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptDefinitionUnfolderManchesterSyntax, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptDefinitionUnfolderManchesterSyntax, conceptName2);

        return computeSimilarity(dynamicProgrammingSimPiReasonerImpl, superRoleUnfolderManchesterSyntax, tree1, tree2);
    }

    public BigDecimal measureKRSSConcetpsWithTopDownSim(String conceptName1, String conceptName2) throws IOException {
        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with top down Sim as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName2);

        return computeSimilarity(topDownSimReasonerImpl, superRoleUnfolderKRSSSyntax, tree1, tree2);
    }

    public BigDecimal measureKRSSConceptsWithTopDownSimPi(String conceptName1, String conceptName2) throws IOException {
        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with top down SimPi as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName2);

        return computeSimilarity(topDownSimPiReasonerImpl, superRoleUnfolderKRSSSyntax, tree1, tree2);
    }

    public BigDecimal measureKRSSConceptsWithDynamicProgrammingSim(String conceptName1, String conceptName2) throws IOException {
        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with dynamic programming Sim as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptDefinitionUnfolderKRSSSyntax, conceptName2);

        return computeSimilarity(dynamicProgrammingSimReasonerImpl, superRoleUnfolderKRSSSyntax, tree1, tree2);
    }

    public BigDecimal measureConceptWithType(String conceptName1, String conceptName2, TypeConstant type, String conceptType) throws IOException {
        IConceptUnfolder conceptT = null;

        IRoleUnfolder roleUnfolderT = null;

        IReasoner reasonerT = null;

        String measurementType = type.getDescription();

        if (conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable measure with " + measurementType + " as conceptName1[" + conceptName1 + "] and " +
                    "conceptName2[" + conceptName2 + "] are null.", ErrorCode.OWLSimService_IllegalArguments);
        }

        if (conceptType.equals("KRSS")) {
            conceptT = conceptDefinitionUnfolderKRSSSyntax;
            roleUnfolderT = superRoleUnfolderKRSSSyntax;
        } else if (conceptType.equals("OWL")) {
            conceptT = conceptDefinitionUnfolderManchesterSyntax;
            roleUnfolderT = superRoleUnfolderManchesterSyntax;
        }

        if (measurementType.equals("dynamic programming Sim")) {
            reasonerT = dynamicProgrammingSimReasonerImpl;
        } else if (measurementType.equals("dynamic programming SimPi")) {
            reasonerT = dynamicProgrammingSimPiReasonerImpl;
        } else if (measurementType.equals("top down Sim")) {
            reasonerT = topDownSimReasonerImpl;
        } else {
            reasonerT = topDownSimPiReasonerImpl;
        }

        Tree<Set<String>> tree1 = unfoldAndConstructTree(conceptT, conceptName1);
        Tree<Set<String>> tree2 = unfoldAndConstructTree(conceptT, conceptName2);

        BigDecimal result = computeSimilarity(reasonerT, roleUnfolderT, tree1, tree2);

        // extract explanation
        backTraceTable.inputConceptName(conceptName1, conceptName2);
        backTraceTable.inputTreeNodeValue(tree1, result, 1);
        backTraceTable.inputTreeNodeValue(tree2, result, 2);

        explanationService.explainSimilarity(backTraceTable);

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Removed later ///////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Map<String, List<String>>> getTopDownSimExecutionMap() {
        return topDownSimExecutionMap;
    }

    public Map<String, Map<String, List<String>>> getTopDownSimPiExecutionMap() {
        return topDownSimPiExecutionMap;
    }

    public Map<String, Map<String, List<String>>> getDynamicProgrammingSimExecutionMap() {
        return dynamicProgrammingSimExecutionMap;
    }

    public Map<String, Map<String, List<String>>> getDynamicProgrammingSimPiExecutionMap() {
        return dynamicProgrammingSimPiExecutionMap;
    }
}
