plugins {
    java
    application
    id("com.gradleup.shadow") version "9.3.2"
}

group = "dev.rezu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
        attributes["Main-Class"] = "dev.rezu.Server"
    }
}

tasks.shadowJar {
    archiveBaseName.set("workout-helper-server")
    archiveVersion.set("")

    manifest {
        attributes(
            "Main-Class" to "dev.rezu.Server"
        )
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
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "--enable-preview"))
}
// to prevent alerts about native access
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-XX:+UseCompactObjectHeaders", "--enable-preview")
}