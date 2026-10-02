plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "9.8.0"
}

// Toolchains:
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// Dependencies:
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.1")
    implementation("net.java.dev.jna:jna:5.18.1")

    implementation("com.github.hypfvieh:dbus-java:3.3.2")

    compileOnly("org.jetbrains:annotations:26.0.2")
}

// Task:
tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.test {
    failOnNoDiscoveredTests.set(false) // all tests are demos
}

// Publishing:
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "me.darragh"
            artifactId = "playing-api-java8"
            version = project.version.toString()

            pom {
                name.set("playing-api")
                properties.set(mapOf(
                    "java.version" to "8",
                    "project.build.sourceEncoding" to "UTF-8",
                    "project.reporting.outputEncoding" to "UTF-8"
                ))
                developers {
                    developer {
                        id.set("darraghd493")
                        name.set("Darragh")
                    }
                }
                organization {
                    name.set("darragh.website")
                    url.set("https://darragh.website")
                }
                scm {
                    connection.set("scm:git:git://github.com/etherclient/playing-api.git")
                    developerConnection.set("scm:git:ssh://github.com/etherclient/playing-api.git")
                    url.set("https://github.com/etherclient/playing-api")
                }
            }

            java {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            url = uri("https://repo.darragh.website/releases")
            credentials {
                username = providers.gradleProperty("repoToken").orNull // not ideal but lazy for github actions lol
                password = providers.gradleProperty("repoSecret").orNull
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}