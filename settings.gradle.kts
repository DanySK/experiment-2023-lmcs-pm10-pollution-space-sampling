plugins {
    id("com.gradle.enterprise") version "3.12.4"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishOnFailure()
    }
}

rootProject.name = "experiment-2023-lmcs-pm10-pollution-space-sampling"
