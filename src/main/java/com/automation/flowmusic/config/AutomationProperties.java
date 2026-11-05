package com.automation.flowmusic.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "automation")
public class AutomationProperties {

    @Valid
    @NotNull
    private Browser browser = new Browser();

    @Valid
    @NotNull
    private Selectors selectors = new Selectors();

    @Valid
    @NotNull
    private Timing timing = new Timing();

    @Valid
    @NotNull
    private Retry retry = new Retry();

    @Valid
    @NotNull
    private Generation generation = new Generation();

    @Valid
    @NotNull
    private Download download = new Download();

    @Data
    public static class Browser {
        @NotBlank(message = "cdpUrl must not be blank")
        private String cdpUrl;

        @NotBlank(message = "flowMusicUrl must not be blank")
        private String flowMusicUrl;

        @Min(value = 1000, message = "navigationTimeoutMs must be >= 1000")
        private long navigationTimeoutMs = 30000;
    }

    @Data
    public static class Selectors {
        @NotBlank(message = "promptInput selector must not be blank")
        private String promptInput;

        @NotBlank(message = "generateButton selector must not be blank")
        private String generateButton;

        @NotBlank(message = "audioCard selector must not be blank")
        private String audioCard;

        @NotBlank(message = "downloadButton selector must not be blank")
        private String downloadButton;

        @NotBlank(message = "loadingIndicator selector must not be blank")
        private String loadingIndicator;
    }

    @Data
    public static class Timing {
        @Min(value = 0, message = "jitterMinMs must be >= 0")
        private long jitterMinMs = 4000;

        @Min(value = 1, message = "jitterMaxMs must be >= 1")
        private long jitterMaxMs = 9000;

        @Min(value = 0, message = "batchCooldownMs must be >= 0")
        private long batchCooldownMs = 15000;

        @Min(value = 1000, message = "generationTimeoutMs must be >= 1000")
        private long generationTimeoutMs = 120000;

        @AssertTrue(message = "jitterMinMs must be less than or equal to jitterMaxMs")
        public boolean isValidJitterRange() {
            return jitterMinMs <= jitterMaxMs;
        }
    }

    @Data
    public static class Retry {
        @Min(value = 1, message = "maxAttempts must be >= 1")
        private int maxAttempts = 3;

        @Min(value = 0, message = "backoffBaseSeconds must be >= 0")
        private long backoffBaseSeconds = 2;
    }

    @Data
    public static class Generation {
        @Min(value = 1, message = "defaultTargetCount must be >= 1")
        private int defaultTargetCount = 30;

        @NotBlank(message = "instrumentalSuffix must not be blank")
        private String instrumentalSuffix = ", strictly instrumental, no vocals, no lyrics, no singing, high quality studio audio";

        @Min(value = 100, message = "maxPromptLength must be >= 100")
        private int maxPromptLength = 1000;
    }

    @Data
    public static class Download {
        @Min(value = 1, message = "minFileSizeBytes must be >= 1")
        private long minFileSizeBytes = 10240;

        @Min(value = 1000, message = "fileWaitTimeoutMs must be >= 1000")
        private long fileWaitTimeoutMs = 60000;
    }
}
