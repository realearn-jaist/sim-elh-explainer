package io.github.xlives.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class OWLCLassExtractorApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(OWLClassExtractor.class, args);

        // Get the OWLClassExtractor bean and run it
        OWLClassExtractor extractor = context.getBean(OWLClassExtractor.class);

        try {
            extractor.extractAndWriteClasses();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
