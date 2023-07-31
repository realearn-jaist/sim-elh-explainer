package io.github.xlives.service;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;

public class KRSSConverter {
    public static void convertKRSSFileToOWL(String krssFilePath, String outputOWLFilePath) {
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(krssFilePath));

            // perform conversion and processing of KRSS content to OWL classes, individuals, properties, axioms, etc.
            RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
            File outputOWLFile = new File(outputOWLFilePath);
            manager.saveOntology(ontology, format, IRI.create(outputOWLFile.toURI()));

            System.out.println("Conversion successful. OWL ontology saved to: " + outputOWLFilePath);
        } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            e.printStackTrace();
            System.err.println("Error converting KRSS to OWL: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String krssFilePath = "/Users/rchn/Desktop/refactor/sim-preference-elh/batch-krss-dynamicprogramming-sim/input/snomed.krss";
        String outputOWLFilePath = "/Users/rchn/Desktop/refactor/sim-preference-elh/snomed.owl";

        convertKRSSFileToOWL(krssFilePath, outputOWLFilePath);
    }
}
