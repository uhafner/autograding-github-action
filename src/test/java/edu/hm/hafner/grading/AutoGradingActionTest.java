package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import edu.hm.hafner.util.ResourceTest;

import static org.assertj.core.api.Assertions.*;

class AutoGradingActionTest extends ResourceTest {
    @Test
    void shouldReadDefaultConfigurationIfEnvironmentIsNotSet() {
        var action = new AutoGradingAction();

        assertThat(action.getConfiguration()).contains(
                toString("/default-config.json"));
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = "{}")
    void shouldReadConfigurationFromEnvironment() {
        var action = new AutoGradingAction();

        assertThat(action.getConfiguration()).isEqualTo("{}");
    }
}
