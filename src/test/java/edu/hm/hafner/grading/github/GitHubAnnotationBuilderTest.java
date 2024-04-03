package edu.hm.hafner.grading.github;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.util.FilteredLog;

import org.kohsuke.github.GHCheckRunBuilder.Annotation;
import org.kohsuke.github.GHCheckRunBuilder.Output;

import static org.mockito.Mockito.*;

class GitHubAnnotationBuilderTest {
    @Test
    void shouldSkipAnnotationsWhenEmpty() {
        var log = new FilteredLog("unused");
        var output = mock(Output.class);

        new GitHubAnnotationsBuilder(output, "/tmp", log).createAnnotations(new AggregatedScore("{}", log));

        verify(output, never()).add(any(Annotation.class));
    }
}
