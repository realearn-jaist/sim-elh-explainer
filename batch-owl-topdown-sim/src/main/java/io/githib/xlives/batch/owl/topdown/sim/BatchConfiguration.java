package io.githib.xlives.batch.owl.topdown.sim;

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
import java.util.ArrayList;
import java.util.List;

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
    private static String PATH_OWL_ONTOLOGY =null;
    private static File INPUT_CONCEPTS = null;
    private static File OUTPUT_TOPDOWN_SIM = null;

    @Autowired
    private Environment env ;

    // runchana:2023-09-22 Configuration path using environment in application.properties
    @PostConstruct
    public void init() {
        String inputConceptsPath = env.getProperty("inputConcepts.TopDownSim");
        String outputTopDownPath = env.getProperty("output.TopDownSim");
        String inputOntologyPath = env.getProperty("inputOntology.TopDownSim");

        if (inputConceptsPath != null && outputTopDownPath != null) {
            INPUT_CONCEPTS = new File(inputConceptsPath);
            OUTPUT_TOPDOWN_SIM = new File(outputTopDownPath);
            PATH_OWL_ONTOLOGY = inputOntologyPath;
        } else {
            throw new IllegalStateException("Path is not properly configured.");
        }
    }

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private OWLServiceContext owlServiceContext;
    @Autowired
    private OWLSimilarityController owlSimilarityController;

    private List<String> concept1sToMeasure;
    private List<String> concept2sToMeasure;
    private StringBuilder topDownSimResult;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    protected Tasklet taskComputeTopDownSim() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                topDownSimResult = new StringBuilder();

                for (int i = 0; i < concept1sToMeasure.size(); i++) {
                    topDownSimResult.append(concept1sToMeasure.get(i));
                    topDownSimResult.append("\t");
                    topDownSimResult.append(concept2sToMeasure.get(i));
                    topDownSimResult.append("\t");
                    // runchana:2023-31-07 invoke refactored method with new params to specify measurement and concept type
                    topDownSimResult.append(owlSimilarityController.measureSimilarity(concept1sToMeasure.get(i), concept2sToMeasure.get(i), TypeConstant.TOPDOWN_SIM, "OWL"));

                    List<String> benchmark = owlSimilarityController.getTopDownSimExecutionMap().get(concept1sToMeasure.get(i) + " tree").get(concept2sToMeasure.get(i) + " tree");
                    for (String result : benchmark) {
                        topDownSimResult.append("\t");
                        topDownSimResult.append(result);
                    }

                    topDownSimResult.append("\n");
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
    protected Tasklet taskWriteTopDownSimToFile() {

        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                FileUtils.writeStringToFile(OUTPUT_TOPDOWN_SIM, topDownSimResult.toString(), false);

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
    protected Step stepReadInputOWLOntology() throws Exception {
        return this.stepBuilderFactory.get("stepReadInputOWLOntology").tasklet(taskReadInputOWLOntology()).build();
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
                .next(stepReadInputOWLOntology())
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
