package io.github.xlives.controller;

import io.github.xlives.enumeration.TypeConstant;
import io.github.xlives.exception.ErrorCode;
import io.github.xlives.exception.JSimPiException;
import io.github.xlives.service.SimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class KRSSSimilarityController {

    @Autowired
    private SimilarityService similarityService;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BigDecimal measureSimilarity(String conceptName1, String conceptName2, TypeConstant type, String conceptType) throws IOException {
        if(conceptName1 == null || conceptName2 == null) {
            throw new JSimPiException("Unable to measure similarity with " + type.getDescription() + " as conceptName1[" + conceptName1
                    + "] and conceptName2[" + conceptName2 + "] are null.",
                    ErrorCode.OwlSimilarityController_IllegalArguments);
        }

        BigDecimal value = similarityService.measureConceptWithType(conceptName1, conceptName2, type, conceptType);

        return value.setScale(5, BigDecimal.ROUND_HALF_UP);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Benchmarks //////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Map<String, List<String>>> getTopDownSimExecutionMap() {
        return similarityService.getTopDownSimExecutionMap();
    }

    public Map<String, Map<String, List<String>>> getTopDownSimPiExecutionMap() {
        return similarityService.getTopDownSimPiExecutionMap();
    }

    public Map<String, Map<String, List<String>>> getDynamicProgrammingSimExecutionMap() {
        return similarityService.getDynamicProgrammingSimExecutionMap();
    }

    public Map<String, Map<String, List<String>>> getDynamicProgrammingSimPiExecutionMap() {
        return similarityService.getDynamicProgrammingSimPiExecutionMap();
    }

}
