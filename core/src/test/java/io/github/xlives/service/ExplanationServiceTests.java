package io.github.xlives.service;

import io.github.xlives.enumeration.TypeConstant;
import io.github.xlives.framework.KRSSServiceContext;
import io.github.xlives.framework.OWLServiceContext;
import io.github.xlives.framework.PreferenceProfile;
import io.github.xlives.framework.descriptiontree.TreeBuilder;
import io.github.xlives.framework.reasoner.*;
import io.github.xlives.framework.unfolding.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SimilarityService.class, SuperRoleUnfolderManchesterSyntax.class,
        TopDownSimReasonerImpl.class, TopDownSimPiReasonerImpl.class,
        DynamicProgrammingSimReasonerImpl.class, DynamicProgrammingSimPiReasonerImpl.class,
        ConceptDefinitionUnfolderManchesterSyntax.class, ConceptDefinitionUnfolderKRSSSyntax.class,
        SuperRoleUnfolderKRSSSyntax.class,
        TreeBuilder.class, OWLServiceContext.class, KRSSServiceContext.class,
        PreferenceProfile.class
})

/**
 * runchana:2023-08-02
 * A test for explanation service
 */
public class ExplanationServiceTests {

    private static final String OWL_FILE_PATH = "family.owl";

    private static final String EXPLANATION_PATH = "/Users/rchn/Desktop/refactor/sim-elh-explainer/explanation/explanation.txt";

    private static List<String> result;

    private static final String FRESH_PRIMITIVE_CONCEPT_NAME_FEMALE = "Female'";
    private static final String FRESH_PRIMITIVE_CONCEPT_NAME_MALE = "Male'";

    private static final String ROLE_HAS_CHILD = "hasChild";
    private static final String ROLE_HAS_PARENT = "hasParent";
    private static final String ROLE_HAS_SON = "hasSon";
    private static final String ROLE_IS_FATHER_OF = "isFatherOf";

    private static final String PRIMITIVE_ROLE_NAME_HAS_PARENT = "hasParent'";
    private static final String PRIMITIVE_ROLE_NAME_HAS_ANCESTOR = "hasAncestor'";
    private static final String PRIMITIVE_ROLE_NAME_IS_BLOOD_RELATION_OF = "isBloodRelationOf'";
    private static final String PRIMITIVE_ROLE_NAME_IS_RELATION_OF = "isRelationOf";

    private Map<String, String> fullConceptDefinitionMap = new HashMap<String, String>();
    private Map<String, String> primitiveConceptDefinitionMap = new HashMap<String, String>();
    private Map<String, String> primitiveRoleDefinitionMap = new HashMap<String, String>();

    @Autowired
    private SimilarityService similarityService;

    @Mock
    private KRSSServiceContext krssServiceContext;

    @Autowired
    private OWLServiceContext OWLServiceContext;

    @Autowired
    private PreferenceProfile preferenceProfile;

    @Before
    public void init() throws IOException {
        OWLServiceContext.init(OWL_FILE_PATH);

        // Populate primitive concept importance
        this.preferenceProfile.addPrimitiveConceptImportance(FRESH_PRIMITIVE_CONCEPT_NAME_FEMALE, new BigDecimal("2"));
        this.preferenceProfile.addPrimitiveConceptImportance(FRESH_PRIMITIVE_CONCEPT_NAME_MALE, new BigDecimal("2"));

        // Populate role importance
        this.preferenceProfile.addRoleImportance(ROLE_HAS_CHILD, BigDecimal.ZERO);
        this.preferenceProfile.addRoleImportance(ROLE_HAS_PARENT, new BigDecimal("2"));
        this.preferenceProfile.addRoleImportance(ROLE_HAS_SON, new BigDecimal("2"));
        this.preferenceProfile.addRoleImportance(PRIMITIVE_ROLE_NAME_IS_RELATION_OF, new BigDecimal("2"));

        // Populate primitive concepts similarity
        this.preferenceProfile.addPrimitveConceptsSimilarity("Female'", "Male'", new BigDecimal("0.2"));

        // Populate roles similarity
        this.preferenceProfile.addPrimitiveRolesSimilarity(ROLE_IS_FATHER_OF, PRIMITIVE_ROLE_NAME_HAS_PARENT, new BigDecimal("0.6"));
        this.preferenceProfile.addPrimitiveRolesSimilarity(ROLE_IS_FATHER_OF, PRIMITIVE_ROLE_NAME_HAS_ANCESTOR, new BigDecimal("0.4"));
        this.preferenceProfile.addPrimitiveRolesSimilarity(ROLE_IS_FATHER_OF, PRIMITIVE_ROLE_NAME_IS_BLOOD_RELATION_OF, new BigDecimal("0.2"));
        this.preferenceProfile.addPrimitiveRolesSimilarity(ROLE_IS_FATHER_OF, PRIMITIVE_ROLE_NAME_IS_RELATION_OF, new BigDecimal("0.6"));

        // Populate role discount factor
        this.preferenceProfile.addRoleDiscountFactor(ROLE_HAS_PARENT, new BigDecimal("0.2"));

        // Populate full concept definition map
        this.fullConceptDefinitionMap.put("Man", "(and Male Person)");
        this.fullConceptDefinitionMap.put("Son", "(and Man (some hasParent Person))");
        this.fullConceptDefinitionMap.put("SonInLaw", "(and Man (some hasParent (and Person (some isSpouseOf Person))))");
        this.fullConceptDefinitionMap.put("Grandfather", "(and Man (some isFatherOf (and Person (some isParentOf Person))))");

        // Populate primitive concept definition map
        this.primitiveConceptDefinitionMap.put("Male", "(and Male' Sex)");
        this.primitiveConceptDefinitionMap.put("Sex", "(and Sex' Thing)");
        this.primitiveConceptDefinitionMap.put("Female", "(and Female' Sex)");

        // Populate primitive role map
        this.primitiveRoleDefinitionMap.put("hasParent", "(and hasParent' hasAncestor)");
        this.primitiveRoleDefinitionMap.put("hasAncestor", "(and hasAncestor' isBloodRelationOf)");
        this.primitiveRoleDefinitionMap.put("isBloodRelationOf", "(and isBloodRelationOf' isRelationOf)");
        this.primitiveRoleDefinitionMap.put("isSpouseOf", "(and isSpouseOf' isInLawOf)");
        this.primitiveRoleDefinitionMap.put("isInLawOf", "(and isInLawOf' isRelationOf)");

        MockitoAnnotations.initMocks(this);

        when(krssServiceContext.getFullConceptDefinitionMap()).thenReturn(fullConceptDefinitionMap);
        when(krssServiceContext.getPrimitiveConceptDefinitionMap()).thenReturn(primitiveConceptDefinitionMap);
        when(krssServiceContext.getPrimitiveRoleDefinitionMap()).thenReturn(primitiveRoleDefinitionMap);

        similarityService.measureConceptWithType("SonInLaw", "Grandfather", TypeConstant.TOPDOWN_SIMPI, "OWL");
        similarityService.measureConceptWithType("Son", "SonInLaw", TypeConstant.TOPDOWN_SIM, "KRSS");
        similarityService.measureConceptWithType("Female", "Person", TypeConstant.DYNAMIC_SIM, "OWL");
        similarityService.measureConceptWithType("Son", "Man", TypeConstant.DYNAMIC_SIM, "KRSS");

        result = readTextFile();
    }

    /**
     * runchana:2023-08-02
     * Retrieve the explanation text file and extract only a homomorphism degree from each pair of concepts
     */
    @Test
    public void testDegreeOutput() {

        String hd1 = extractString(result.get(0), "DEGREE"); // 1st pair
        assertEquals(hd1, "0.84995");

        String hd2 = extractString(result.get(3), "DEGREE"); // 2nd pair
        assertEquals(hd2, "0.97000");

        String hd3 = extractString(result.get(6), "DEGREE"); // 3rd pair
        assertEquals(hd3, "0.00000");

        String hd4 = extractString(result.get(9), "DEGREE"); // 4th pair
        assertEquals(hd4, "0.90000");

    }

    /**
     * runchana:2023-08-02
     * Check whether the matched concepts in explanation lists is correct or not
     */
    @Test
    public void testMatchedConcepts() throws IOException {

        String concepts1 = extractString(result.get(0), "CONCEPT"); // 1st pair
        assertEquals(concepts1, "[Sex, Male, Person, Thing]");

        String concepts2 = extractString(result.get(3), "CONCEPT"); // 2nd pair
        assertEquals(concepts2, "[Sex, Male, Person, Thing]");

        String concepts3 = extractString(result.get(6), "CONCEPT"); // 3rd pair
        assertEquals(concepts3, "[nothing]");

        String concepts4 = extractString(result.get(9), "CONCEPT"); // 4th pair
        assertEquals(concepts4, "[Sex, Male, Person, Thing]");

    }

    /**
     * runchana:2023-08-02
     * Check homomorphism degree extracted from the explanation
     * @param input
     * @return
     */
    public static String extractString(String input, String type) {
        Pattern pattern = null;
        Matcher matcher = null;

        String res = null;

        if (Objects.equals(type, "DEGREE")) {
            // The similarity between CONCEPT1 and CONCEPT2 is...
            pattern = Pattern.compile("(?<=is\\s)\\d+(\\.\\d+)?");
        } else if (Objects.equals(type, "CONCEPT")) {
            // CONCEPT NAME = [CONCEPTs], [ROLEs]
            pattern = Pattern.compile("\\[(.*?)\\]");
        }

        if (pattern != null){
            matcher = pattern.matcher(input);
        }

        if (matcher != null && matcher.find()) {
            res = matcher.group();
        }

        return res;
    }

    /**
     * runchana:2023-02-08
     * Read a text file
     * @return
     */
    public static List<String> readTextFile() {
        List<String> explanationTexts = new ArrayList<>();

        try (FileReader fileReader = new FileReader(EXPLANATION_PATH);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                explanationTexts.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return explanationTexts;
    }

}
