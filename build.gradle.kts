val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val mockk_version: String by project

plugins {
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22"
}

group = "me.leonunes"
version = "0.0.1"
application {
    mainClass.set("me.leonunes.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:${mockk_version}")
}

ktor {
    fatJar {
        archiveFileName.set("server.jar")
    }
}

tasks.register<Zip>("buildElasticBeanstalkZip") {
    dependsOn("buildFatJar")

    archiveFileName.set("elasticbeanstalk.zip")
    destinationDirectory.set(layout.buildDirectory.dir("aws"))

    from(layout.buildDirectory.file("libs/server.jar"))
    from(layout.projectDirectory.dir(".platform")) {
        into(".platform")
    }
    from(layout.projectDirectory.dir(".ebextensions")) {
        into(".ebextensions")
    }
}
