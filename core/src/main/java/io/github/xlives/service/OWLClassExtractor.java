package io.github.xlives.service;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * runchana:23-31-07
 * The OWLClassExtractor class extracts every concept name that is listed in .owl file and appends them into a new file as initialized in 'OutputFile'.
 * An input 'owlFilePath' should be .owl file.
 */
@Component("owlClassExtractor")
public class OWLClassExtractor {

    @Autowired
    private Environment env;

    @PostConstruct
    public void extractAndWriteClasses() throws IOException {

        // runchana:2023-09-16 Set file paths in application.properties
        String owlFilePath = env.getProperty("owlFilePath");
        String outputFilePath = env.getProperty("outputFilePath");

        File OutputFile = new File(outputFilePath);

        StringBuilder ResultOutput;
        ResultOutput = new StringBuilder();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(IRI.create("file:" + owlFilePath));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return;
        }

        ShortFormProvider shortFormProvider = new SimpleShortFormProvider();

        // To extract
        for (OWLClass owlClass1 : ontology.getClassesInSignature()) {
            String className1 = shortFormProvider.getShortForm(owlClass1);

            for (OWLClass owlClass2 : ontology.getClassesInSignature()) {
                String className2 = shortFormProvider.getShortForm(owlClass2);

                // runchana:2023-08-01 skip pairing if className1 is equal to or comes after className2 lexicographically/equal to thing
                if (className1.equals(className2) || className1.compareTo(className2) >= 0 || className1.equals("Thing") || className2.equals("Thing")) {
                    continue;
                }
                System.out.println(className1 + " " + className2);
                ResultOutput.append(className1 + " " + className2);
                ResultOutput.append("\n");
            }
        }

        FileUtils.writeStringToFile(OutputFile, ResultOutput.toString(), false);
    }
}