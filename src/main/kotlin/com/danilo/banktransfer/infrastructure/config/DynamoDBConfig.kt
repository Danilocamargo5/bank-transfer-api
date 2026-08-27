package com.danilo.banktransfer.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@Configuration
class DynamoDBConfig(
    @Value("\${aws.dynamodb.endpoint}")
    private val endpoint: String,
    @Value("\${aws.dynamodb.region}")
    private val region: String,
    @Value("\${aws.access.key.id}")
    private val accessKeyId: String,
    @Value("\${aws.secret.access.key}")
    private val secretAccessKey: String
) {

    @Bean
    fun dynamoDbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            )
            .build()
    }
}
