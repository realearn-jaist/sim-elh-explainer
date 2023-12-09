package io.github.xlives.batch.krss.topdown.sim;

import io.github.xlives.controller.KRSSSimilarityController;
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
import java.util.List;

@Import(value= {KRSSSimilarityController.class, ValidationService.class,
        TopDownSimReasonerImpl.class, TopDownSimPiReasonerImpl.class,
        DynamicProgrammingSimReasonerImpl.class, DynamicProgrammingSimPiReasonerImpl.class,
        ConceptDefinitionUnfolderManchesterSyntax.class, ConceptDefinitionUnfolderKRSSSyntax.class,
        TreeBuilder.class, OWLServiceContext.class, KRSSServiceContext.class,
        SuperRoleUnfolderKRSSSyntax.class,
        SimilarityService.class, SuperRoleUnfolderManchesterSyntax.class, PreferenceProfile.class
})
@EnableBatchProcessing
@SpringBootApplication

public class BatchConfiguration {

    private static File INPUT_CONCEPTS = null;
    private static File OUTPUT_TOPDOWN_SIM = null;

    private static File OUTPUT_PLUS_FIVE_PERCENT_TOPDOWN_SIM = null;
    private static File OUTPUT_MINUS_FIVE_PERCENT_TOPDOWN_SIM = null;

    private static String PATH_KRSS_ONTOLOGY =null;

    private static final String HEADER_RESULT = "concept" + "\t" + "concept" + "\t" + "similarity" + "\t" + "millisecond";

    @Autowired
    private Environment env ;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KRSSServiceContext krssServiceContext;
    @Autowired
    private KRSSSimilarityController krssSimilarityController;

    private List<String> concept1sToMeasure;
    private List<String> concept2sToMeasure;
    private StringBuilder topDownSimResult;

    // teeradaj@20160608:
    // For conducting experiments only
    private StringBuilder topDownSimResultPlus5Percetage;
    private StringBuilder topDownSimResultMinus5Percatage;

    // runchana:2023-09-22 Configuration path using environment in application.properties
    @PostConstruct
    public void init() {
        String inputConceptsPath = env.getProperty("inputConcepts.TopDownSimKRSS");
        String outputDynamicPath = env.getProperty("output.TopDownSimKRSS");
        String inputKRSSPath = env.getProperty("inputKRSS.TopDownSim");
        String outputPlusFivePercentTopDownSim = env.getProperty("outputPlusFivePercent.TopDownSim");
        String outputMinusFivePercentTopDownSim = env.getProperty("outputMinusFivePercent.TopDownSim");

        if (inputConceptsPath != null && outputDynamicPath != null) {
            INPUT_CONCEPTS = new File(inputConceptsPath);
            OUTPUT_TOPDOWN_SIM = new File(outputDynamicPath);
            PATH_KRSS_ONTOLOGY = inputKRSSPath;
            OUTPUT_PLUS_FIVE_PERCENT_TOPDOWN_SIM = new File(outputPlusFivePercentTopDownSim);
            OUTPUT_MINUS_FIVE_PERCENT_TOPDOWN_SIM = new File(outputMinusFivePercentTopDownSim);
        } else {
            throw new IllegalStateException("Path is not properly configured.");
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    protected Tasklet taskComputeTopDownSim() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                topDownSimResult = new StringBuilder();
                topDownSimResult.append(HEADER_RESULT + "\n");

                // teeradaj@20160608:
                // For conducting experiments only

//                topDownSimResultPlus5Percetage = new StringBuilder();
//                topDownSimResultPlus5Percetage.append(HEADER_RESULT + "\n");
//
//                topDownSimResultMinus5Percatage = new StringBuilder();
//                topDownSimResultMinus5Percatage.append(HEADER_RESULT + "\n");
//
//
//                final BigDecimal onePointZeroFive = new BigDecimal("1.05");
//                final BigDecimal zeroPointNineFive = new BigDecimal("0.95");

                for (int i = 0; i < concept1sToMeasure.size(); i++) {
                    topDownSimResult.append(concept1sToMeasure.get(i));
                    topDownSimResult.append("\t");
                    topDownSimResult.append(concept2sToMeasure.get(i));
                    topDownSimResult.append("\t");

                    // runchana:2023-31-07 invoke refactored method with new params to specify measurement and concept type
                    BigDecimal degree = krssSimilarityController.measureSimilarity(concept1sToMeasure.get(i), concept2sToMeasure.get(i), TypeConstant.TOPDOWN_SIM, "KRSS");

                    topDownSimResult.append(degree);

//                    topDownSimResultPlus5Percetage.append(concept1sToMeasure.get(i));
//                    topDownSimResultPlus5Percetage.append("\t");
//                    topDownSimResultPlus5Percetage.append(concept2sToMeasure.get(i));
//                    topDownSimResultPlus5Percetage.append("\t");
//
//                    BigDecimal plus = degree.multiply(onePointZeroFive);
//                    if (plus.compareTo(BigDecimal.ONE) > 0) {
//                        plus = BigDecimal.ONE;
//                    }
//
//                    else if (plus.compareTo(BigDecimal.ZERO) < 0) {
//                        plus = BigDecimal.ZERO;
//                    }
//
//                    topDownSimResultPlus5Percetage.append(plus.setScale(5, BigDecimal.ROUND_HALF_UP));
//
//                    topDownSimResultMinus5Percatage.append(concept1sToMeasure.get(i));
//                    topDownSimResultMinus5Percatage.append("\t");
//                    topDownSimResultMinus5Percatage.append(concept2sToMeasure.get(i));
//                    topDownSimResultMinus5Percatage.append("\t");
//
//                    BigDecimal minus = degree.multiply(zeroPointNineFive);
//                    if (minus.compareTo(BigDecimal.ONE) > 0) {
//                        minus = BigDecimal.ONE;
//                    }
//
//                    else if (minus.compareTo(BigDecimal.ZERO) < 0) {
//                        minus = BigDecimal.ZERO;
//                    }
//
//                    topDownSimResultMinus5Percatage.append(minus.setScale(5, BigDecimal.ROUND_HALF_UP));
//
                    List<String> benchmark = krssSimilarityController.getTopDownSimExecutionMap().get(concept1sToMeasure.get(i) + " tree").get(concept2sToMeasure.get(i) + " tree");

                    for (String result : benchmark) {
                        topDownSimResult.append("\t");
                        topDownSimResult.append(result);

//                        topDownSimResultPlus5Percetage.append("\t");
//                        topDownSimResultPlus5Percetage.append(result);
//
//                        topDownSimResultMinus5Percatage.append("\t");
//                        topDownSimResultMinus5Percatage.append(result);
                    }

                    topDownSimResult.append("\n");
//                    topDownSimResultPlus5Percetage.append("\n");
//                    topDownSimResultMinus5Percatage.append("\n");
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
    protected Tasklet taskReadInputKRSSOntology() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                krssServiceContext.init(PATH_KRSS_ONTOLOGY);
                krssServiceContext.resetFullConceptDefinitionMap();

                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    protected Tasklet taskWriteTopDownSimToFile() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                FileUtils.writeStringToFile(OUTPUT_TOPDOWN_SIM, topDownSimResult.toString(), false);
//                FileUtils.writeStringToFile(OUTPUT_PLUS_FIVE_PERCENT_TOPDOWN_SIM, topDownSimResultPlus5Percetage.toString(), false);
//                FileUtils.writeStringToFile(OUTPUT_MINUS_FIVE_PERCENT_TOPDOWN_SIM, topDownSimResultMinus5Percatage.toString(), false);

                return RepeatStatus.FINISHED;
            }
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Steps ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    protected Step stepComputeTopDownSim() throws Exception {
        return this.stepBuilderFactory.get("stepComputeTopDownSim").tasklet(taskComputeTopDownSim()).build();
    }

    @Bean
    protected Step stepReadInputConcepts() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputConcepts").tasklet(taskReadInputConcepts()).build();
    }

    @Bean
    protected Step stepReadInputKRSSOntology() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputKRSSOntology").tasklet(taskReadInputKRSSOntology()).build();
    }

    @Bean
    protected Step stepWriteTopDownSimToFile() throws Exception {
        return this.stepBuilderFactory.get("stepWriteTopDownSimToFile").tasklet(taskWriteTopDownSimToFile()).build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Job /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("job")
                .start(stepReadInputConcepts())
                .next(stepReadInputKRSSOntology())
                .next(stepComputeTopDownSim())
                .next(stepWriteTopDownSimToFile())
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
