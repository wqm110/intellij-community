// FIX: Replace 'kotlinOptions' with 'compilerOptions'
// DISABLE_K2_ERRORS
// TODO: KTIJ-32773
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("jvm") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    <caret>kotlinOptions.sourceMapNamesPolicy = "fully-qualified-names"
}