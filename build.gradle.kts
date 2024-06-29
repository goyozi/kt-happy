plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    antlr
    kotlin("jvm") version "1.9.22"
}

group = "io.github.goyozi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages")
}
tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
tasks.jar {
    manifest {
        attributes("Main-Class" to "io.github.goyozi.kthappy.MainKt")
    }
}