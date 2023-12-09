package io.githib.xlives.batch.owl.topdown.simpi;

import io.github.xlives.controller.KRSSSimilarityController;
import io.github.xlives.controller.OWLSimilarityController;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Import(value= {OWLSimilarityController.class, ValidationService.class,
        TopDownSimReasonerImpl.class, TopDownSimPiReasonerImpl.class,
        DynamicProgrammingSimReasonerImpl.class, DynamicProgrammingSimPiReasonerImpl.class,
        ConceptDefinitionUnfolderManchesterSyntax.class, ConceptDefinitionUnfolderKRSSSyntax.class,
        TreeBuilder.class, OWLServiceContext.class, KRSSServiceContext.class,
        SimilarityService.class, SuperRoleUnfolderManchesterSyntax.class,
        SuperRoleUnfolderKRSSSyntax.class, PreferenceProfile.class
})
@EnableBatchProcessing
@SpringBootApplication
public class BatchConfiguration {

    private static String PATH_OWL_ONTOLOGY = null;
    private static File INPUT_CONCEPTS = null;
    private static File OUTPUT_TOPDOWN_SIMPI = null;
    private static File INPUT_PRIMITIVE_CONCEPT_IMPORTANCE = null;
    private static File INPUT_ROLE_IMPORTANCE = null;
    private static File INPUT_PRIMITIVE_CONCEPTS_SIMILARITY = null;
    private static File INPUT_PRIMITIVE_ROLES_SIMILARITY = null;
    private static File INPUT_ROLE_DISCOUNT_FACTOR = null;

    @Autowired
    private Environment env ;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private OWLServiceContext owlServiceContext;
    @Autowired
    private OWLSimilarityController owlSimilarityController;
    @Autowired
    private PreferenceProfile preferenceProfile;

    private List<String> concept1sToMeasure;
    private List<String> concept2sToMeasure;
    private StringBuilder topDownSimPiResult;

    // runchana:2023-09-22 Configuration path using environment in application.properties
    @PostConstruct
    public void init() {
        String inputConceptsPath = env.getProperty("inputConcepts.TopDownSimPi");
        String outputTopDownPath = env.getProperty("output.TopDownSimPi");
        String inputOntologyPath = env.getProperty("inputOntology.TopDownSimPi");

        if (inputConceptsPath != null && outputTopDownPath != null && inputOntologyPath != null) {
            INPUT_CONCEPTS = new File(inputConceptsPath);
            OUTPUT_TOPDOWN_SIMPI = new File(outputTopDownPath);
            PATH_OWL_ONTOLOGY = inputOntologyPath;
        } else {
            throw new IllegalStateException("Path is not properly configured.");
        }

        // runchana:2023-09-22 preference profile in simPi
        String inputPrimitiveConceptImportance_Path = env.getProperty("inputPrimitiveConceptImportance.TopDownSimpi");
        String inputRoleImportance_Path = env.getProperty("inputRoleImportance.TopDownSimpi");
        String inputPrimitiveConceptSimilarity_Path = env.getProperty("inputPrimitiveConceptSimilarity.TopDownSimpi");
        String inputPrimitiveRolesSimilarity_Path = env.getProperty("inputPrimitiveRolesSimilarity.TopDownSimpi");
        String inputRoleDiscountFactor_Path = env.getProperty("inputRoleDiscountFactor.TopDownSimpi");

        if (inputPrimitiveConceptImportance_Path != null && inputRoleImportance_Path != null && inputPrimitiveConceptSimilarity_Path != null
        && inputPrimitiveRolesSimilarity_Path != null && inputRoleDiscountFactor_Path != null) {
            INPUT_PRIMITIVE_CONCEPT_IMPORTANCE = new File(inputPrimitiveConceptImportance_Path);
            INPUT_ROLE_IMPORTANCE = new File(inputRoleImportance_Path);
            INPUT_PRIMITIVE_CONCEPTS_SIMILARITY = new File(inputPrimitiveConceptSimilarity_Path);
            INPUT_PRIMITIVE_ROLES_SIMILARITY = new File(inputPrimitiveRolesSimilarity_Path);
            INPUT_ROLE_DISCOUNT_FACTOR = new File(inputRoleDiscountFactor_Path);
        }  else {
            throw new IllegalStateException("Preference profile path is not properly configured.");
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    protected Tasklet taskComputeTopDownSimPi() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                topDownSimPiResult = new StringBuilder();

                for (int i = 0; i < concept1sToMeasure.size(); i++) {
                    topDownSimPiResult.append(concept1sToMeasure.get(i));
                    topDownSimPiResult.append("\t");
                    topDownSimPiResult.append(concept2sToMeasure.get(i));
                    topDownSimPiResult.append("\t");

                    // runchana:2023-31-07 invoke refactored method with new params to specify measurement and concept type
                    topDownSimPiResult.append(owlSimilarityController.measureSimilarity(concept1sToMeasure.get(i), concept2sToMeasure.get(i), TypeConstant.TOPDOWN_SIMPI, "OWL"));

                    List<String> benchmark = owlSimilarityController.getTopDownSimPiExecutionMap().get(concept1sToMeasure.get(i) + " tree").get(concept2sToMeasure.get(i) + " tree");
                    for (String result : benchmark) {
                        topDownSimPiResult.append("\t");
                        topDownSimPiResult.append(result);
                    }

                    topDownSimPiResult.append("\n");
                }

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    protected Tasklet taskReadInputConcepts() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                String[] lines = StringUtils.split(FileUtils.readFileToString(INPUT_CONCEPTS), "\n");

                concept1sToMeasure = new ArrayList<String>();
                concept2sToMeasure = new ArrayList<String>();

                for (String eachLine : lines) {
                    String[] concepts = StringUtils.split(eachLine);
                    concept1sToMeasure.add(concepts[0]);
                    concept2sToMeasure.add(concepts[1]);
                }

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    protected Tasklet taskReadInputOWLOntology() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                owlServiceContext.init(PATH_OWL_ONTOLOGY);

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    protected Tasklet taskReadInputPreferenceProfile() {
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

                String[] primitiveConceptImportances = StringUtils.split(FileUtils.readFileToString(INPUT_PRIMITIVE_CONCEPT_IMPORTANCE), "\n");
                for (String primitiveConceptImportance : primitiveConceptImportances) {
                    String[] str = StringUtils.split(primitiveConceptImportance);
                    preferenceProfile.addPrimitiveConceptImportance(str[0], new BigDecimal(str[1]));
                }

                String[] roleImportances = StringUtils.split(FileUtils.readFileToString(INPUT_ROLE_IMPORTANCE), "\n");
                for (String roleImportance : roleImportances) {
                    String[] str = StringUtils.split(roleImportance);
                    preferenceProfile.addRoleImportance(str[0], new BigDecimal(str[1]));
                }

                String[] primitiveConceptsSimilarities = StringUtils.split(FileUtils.readFileToString(INPUT_PRIMITIVE_CONCEPTS_SIMILARITY), "\n");
                for (String primitiveConceptsSimilarity : primitiveConceptsSimilarities) {
                    String[] str = StringUtils.split(primitiveConceptsSimilarity);
                    preferenceProfile.addPrimitveConceptsSimilarity(str[0], str[1], new BigDecimal(str[2]));
                }

                String[] primitiveRolesSimilarities = StringUtils.split(FileUtils.readFileToString(INPUT_PRIMITIVE_ROLES_SIMILARITY), "\n");
                for (String primitiveRolesSimilarity : primitiveRolesSimilarities) {
                    String[] str = StringUtils.split(primitiveRolesSimilarity);
                    preferenceProfile.addPrimitiveRolesSimilarity(str[0], str[1], new BigDecimal(str[2]));
                }

                String[] roleDiscountFactors = StringUtils.split(FileUtils.readFileToString(INPUT_ROLE_DISCOUNT_FACTOR), "\n");
                for (String roleDiscountFactor : roleDiscountFactors) {
                    String[] str = StringUtils.split(roleDiscountFactor);
                    preferenceProfile.addRoleDiscountFactor(str[0], new BigDecimal(str[1]));
                }

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    protected Tasklet taskWriteTopDownSimPiToFile() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                FileUtils.writeStringToFile(OUTPUT_TOPDOWN_SIMPI, topDownSimPiResult.toString(), false);

                return RepeatStatus.FINISHED;
            }
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Steps ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    protected Step stepComputeTopDownSimPi() throws Exception {
        return this.stepBuilderFactory.get("stepComputeTopDownSimPi").tasklet(taskComputeTopDownSimPi()).build();
    }

    @Bean
    protected Step stepReadInputConcepts() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputConcepts").tasklet(taskReadInputConcepts()).build();
    }

    @Bean
    protected Step stepReadInputOWLOntology() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputOWLOntology").tasklet(taskReadInputOWLOntology()).build();
    }

    @Bean
    protected Step stepReadInputPreferenceProfile() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputPreferenceProfile").tasklet(taskReadInputPreferenceProfile()).build();
    }

    @Bean
    protected Step stepWriteTopDownSimPiToFile() throws Exception {
        return this.stepBuilderFactory.get("stepWriteTopDownSimPiToFile").tasklet(taskWriteTopDownSimPiToFile()).build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Job /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("job")
                .start(stepReadInputConcepts())
                .next(stepReadInputOWLOntology())
                .next(stepReadInputPreferenceProfile())
                .next(stepComputeTopDownSimPi())
                .next(stepWriteTopDownSimPiToFile())
                .build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Main ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws Exception {
        System.exit(SpringApplication
                .exit(SpringApplication.run(BatchConfiguration.class, args)));
    }
}
