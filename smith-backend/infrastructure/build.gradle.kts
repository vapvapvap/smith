dependencies {
    api(project(":domain"))
    implementation(libs.mybatis.starter)
    implementation(libs.spring.boot.jdbc.starter)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.httpclient5)
    implementation(libs.jackson.databind)
    runtimeOnly(libs.postgresql)
}
