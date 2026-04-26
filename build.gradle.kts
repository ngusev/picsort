plugins {
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    id("org.jetbrains.compose") version "1.10.1"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.uasoft.tools"
version = "1.0.2"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation(compose.desktop.currentOs)
    @Suppress("DEPRECATION")
    implementation(compose.material3)
    implementation("io.github.vinceglb:filekit-core:0.8.8")
}

tasks.shadowJar {
    archiveBaseName.set("picsort-cli")
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "com.uasoft.picsort.MainKt")
    }
    mergeServiceFiles()
}

val guiJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveBaseName.set("picsort-gui")
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes("Main-Class" to "com.uasoft.picsort.GuiLauncher")
    }
    mergeServiceFiles()
}

tasks.build {
    dependsOn(guiJar)
}
