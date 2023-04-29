// Enable Build Scans
// https://docs.gradle.org/current/userguide/github-actions.html#enable_build_scan_publishing
plugins {
    id("com.gradle.enterprise") version("3.9")
}
gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

rootProject.name = "solarnet"

include("pets", "engine", "repl", "canon")
