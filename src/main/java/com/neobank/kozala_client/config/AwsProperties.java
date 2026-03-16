package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    /**
     * Région AWS (ex. eu-west-1, us-east-1). Utilisée par S3 et Rekognition.
     */
    private String region = "eu-west-1";

    /**
     * Clé d'accès IAM (optionnel). Si absent, le SDK utilise les variables d'environnement ou le profil AWS.
     */
    private String accessKeyId;

    /**
     * Secret d'accès IAM (optionnel). Si absent, le SDK utilise les variables d'environnement ou le profil AWS.
     */
    private String secretAccessKey;
}
