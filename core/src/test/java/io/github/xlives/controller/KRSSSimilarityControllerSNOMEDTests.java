package io.github.xlives.controller;


import io.github.xlives.enumeration.TypeConstant;
import io.github.xlives.framework.KRSSServiceContext;
import io.github.xlives.framework.OWLServiceContext;
import io.github.xlives.framework.PreferenceProfile;
import io.github.xlives.framework.descriptiontree.TreeBuilder;
import io.github.xlives.framework.reasoner.DynamicProgrammingSimPiReasonerImpl;
import io.github.xlives.framework.reasoner.DynamicProgrammingSimReasonerImpl;
import io.github.xlives.framework.reasoner.TopDownSimPiReasonerImpl;
import io.github.xlives.framework.reasoner.TopDownSimReasonerImpl;
import io.github.xlives.framework.unfolding.ConceptDefinitionUnfolderKRSSSyntax;
import io.github.xlives.framework.unfolding.ConceptDefinitionUnfolderManchesterSyntax;
import io.github.xlives.framework.unfolding.SuperRoleUnfolderKRSSSyntax;
import io.github.xlives.framework.unfolding.SuperRoleUnfolderManchesterSyntax;
import io.github.xlives.service.SimilarityService;
import io.github.xlives.service.ValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {KRSSSimilarityController.class, ValidationService.class,
        TopDownSimReasonerImpl.class, TopDownSimPiReasonerImpl.class,
        DynamicProgrammingSimReasonerImpl.class, DynamicProgrammingSimPiReasonerImpl.class,
        ConceptDefinitionUnfolderManchesterSyntax.class, ConceptDefinitionUnfolderKRSSSyntax.class,
        TreeBuilder.class, OWLServiceContext.class, KRSSServiceContext.class,
        SuperRoleUnfolderKRSSSyntax.class,
        SimilarityService.class, SuperRoleUnfolderManchesterSyntax.class, PreferenceProfile.class
})
public class KRSSSimilarityControllerSNOMEDTests {


    @Autowired
    private KRSSSimilarityController krssSimilarityController;

    @Autowired
    private KRSSServiceContext krssServiceContext;

    @Before
    public void init() {

        krssServiceContext.init("snomed.krss");
    }

    @Test
    public void testMeasureSimilarityWithOWLSim() throws IOException {
        BigDecimal value2 = krssSimilarityController.measureSimilarity("10001005", "10001005", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value2).isEqualTo(BigDecimal.ONE.setScale(5, BigDecimal.ROUND_HALF_UP).toPlainString());

        BigDecimal value3 = krssSimilarityController.measureSimilarity("10001005", "10002003", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value3).isEqualTo("0.48016");

        BigDecimal value4 = krssSimilarityController.measureSimilarity("10001005", "10006000", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value4).isEqualTo("0.48796");

        BigDecimal value5 = krssSimilarityController.measureSimilarity("10001005", "1001000", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value5).isEqualTo("0.51113");

        BigDecimal value6 = krssSimilarityController.measureSimilarity("308021002", "199388005", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value6).isEqualTo("0.30804");

        BigDecimal value7 = krssSimilarityController.measureSimilarity("308021002", "290065008", TypeConstant.TOPDOWN_SIM, "KRSS");
        assertThat(value7).isEqualTo("0.38095");
    }

}

