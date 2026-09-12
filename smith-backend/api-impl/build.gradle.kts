plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(libs.spring.boot.webmvc.starter)
    implementation(libs.spring.boot.validation.starter)
    implementation(libs.mybatis.starter)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.spring.boot.flyway)
    implementation(libs.springdoc.ui)
    runtimeOnly(libs.postgresql)
}

springBoot {
    mainClass = "com.smith.web.AiAgentApplication"
}
