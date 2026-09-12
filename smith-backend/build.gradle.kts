plugins {
    `java-library`
}

subprojects {
    apply(plugin = "java-library")

    group = "com.smith"
    version = "0.1.0"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 25
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}
