package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "azure.face")
public class AzureFaceProperties {

    private String endpoint = "";
    private String subscriptionKey = "";
}
