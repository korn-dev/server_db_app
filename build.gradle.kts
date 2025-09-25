plugins {
    kotlin("jvm") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.7.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

application {
    mainClass.set("MainKt")
}