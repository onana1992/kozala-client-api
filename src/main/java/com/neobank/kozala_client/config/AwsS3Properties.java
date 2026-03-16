package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    /**
     * Nom du bucket S3 pour les documents d'identité et selfies.
     */
    private String bucketIdentity = "";
}
