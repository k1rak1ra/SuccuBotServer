import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val sqlite_version: String by project
val postgres_version: String by project
val org_json_version: String by project
val bcrypt_version: String by project
val paho_version: String by project
val bc_version: String by project
val ldaptive_version: String by project
val gson_version: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.k1ra"
version = "1.0.0"

application {
    mainClass.set("net.k1ra.succubotserver.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-jetty-jvm:$ktor_version")
    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.google.code.gson:gson:$gson_version")
    implementation("org.json:json:$org_json_version")

    implementation("org.mindrot:jbcrypt:$bcrypt_version")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.xerial:sqlite-jdbc:$sqlite_version")
    implementation("org.postgresql:postgresql:$postgres_version")

    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:$paho_version")
    implementation("org.bouncycastle:bcprov-jdk18on:$bc_version")

    implementation("org.ldaptive:ldaptive:$ldaptive_version")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.register<ExecutableJarTask>("exec-jar") {
    dependsOn("buildFatJar")
}

tasks.withType<ShadowJar>{
    mergeServiceFiles()
}

abstract class ExecutableJarTask: DefaultTask() {
    // This custom task will prepend the content of a
    // bash launch script at the beginning of a jar,
    // and make it executable (chmod +x)

    @InputFiles
    var originalJars: ConfigurableFileTree = project.fileTree("${project.buildDir}/libs") { include("*-all.jar") }

    @OutputDirectory
    var outputDir: File = project.buildDir.resolve("bin") // where to write the modified jar(s)

    @InputFile
    var launchScript: File = project.rootDir.resolve("launch.sh") // script to prepend

    @TaskAction
    fun createExecutableJars() {
        project.mkdir(outputDir)
        originalJars.forEach { jar ->
            outputDir.resolve(jar.name.replace(".jar","").replace("-all","")).run {
                outputStream().use { out ->
                    out.write(launchScript.readBytes())
                    out.write(jar.readBytes())
                }
                setExecutable(true)
                println("created executable: $path")
            }
        }
    }
}
