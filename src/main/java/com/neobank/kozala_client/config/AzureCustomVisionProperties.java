package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "azure.custom-vision")
public class AzureCustomVisionProperties {

    private String endpoint = "";
    private String predictionKey = "";
    private String projectId = "";
    private String publishedName = "Iteration1";
}
