import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//import com.floern.castingcsv.CsvConfig
plugins {
    kotlin("jvm") version "1.6.21"
}

group = "me.laura"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.anwiba.database:anwiba-database-sqlite:1.1.158")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.exposed", "exposed-core", "0.38.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.38.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.38.1")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")//sqlite
    implementation("org.apache.commons:commons-csv:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
