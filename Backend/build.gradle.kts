plugins {
    java
    application
}

group = "dev.rezu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    // Database driver
    implementation("com.mysql:mysql-connector-j:9.6.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Set main class for application plugin
application {
    mainClass = "dev.rezu.Server"
}

tasks.jar {
    archiveBaseName = "workout-helper-server"
    archiveVersion = ""
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "dev.rezu.Server",
            "Implementation-Title" to "FitGet Workout Manager",
            "Implementation-Version" to version
        )
    }
    
    // Include all runtime dependencies in the JAR
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { 
            if (it.isDirectory) it else zipTree(it) 
        }
    })
}

// Ensure static resources are included
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from("src/main/resources") {
        include("static/**")
        include("*.p12") // Include keystore if in resources
    }
}

// Test configuration
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Helper task to run the server (optional)
tasks.register("runServer") {
    group = "application"
    dependsOn("run")
}

// Clean up - remove logs on clean
tasks.named("clean") {
    doLast {
        delete("logs")
        delete("*.log")
    }
}

// to see deprecated alerts
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
// to prevent alerts about native access
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-XX:+UseCompactObjectHeaders")
}