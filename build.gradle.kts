import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.lwjgl.Lwjgl

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.lwjgl.plugin") version "0.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    Lwjgl {
        implementation(Lwjgl.Preset.minimalOpenGL)
    }

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
