plugins {
	kotlin("jvm") version "2.0.0"
	kotlin("plugin.spring") version "2.0.0"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.danilo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	
	// Spring Retry
	implementation("org.springframework.retry:spring-retry")
	implementation("org.aspectj:aspectjweaver:1.9.22")
	
	// Kafka
	implementation("org.springframework.kafka:spring-kafka")
	
	// AWS DynamoDB
	implementation("software.amazon.awssdk:dynamodb:2.25.0")
	implementation("software.amazon.awssdk:dynamodb-enhanced:2.25.0")
	
	// AWS SQS
	implementation("software.amazon.awssdk:sqs:2.25.0")
	
	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	
	// Logging
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
	
	// Metrics
	implementation("io.micrometer:micrometer-core")
	
	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.testcontainers:testcontainers:1.19.7")
	testImplementation("org.testcontainers:junit-jupiter:1.19.7")
	testImplementation("io.mockk:mockk:1.13.10")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "21"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
