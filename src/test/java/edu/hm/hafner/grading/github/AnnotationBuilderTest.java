package edu.hm.hafner.grading.github;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class AnnotationBuilderTest {
    @Test
    void shouldSkipAnnotationsWhenEmpty() {
        var log = new FilteredLog("unused");
        var annotations = new AnnotationBuilder().createAnnotations(new AggregatedScore("{}", log), log);

        assertThat(annotations).isEmpty();
    }
}
