plugins {
    id("java")
    id("maven-publish")
}

// Toolchains:
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Dependencies:
repositories {
    mavenCentral()
    maven { url = uri("https://raw.githubusercontent.com/jitsi/jitsi-maven-repository/master/releases/") }
}

val annotationImplementation: Configuration by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
    configurations.testCompileOnly.get().extendsFrom(this)
    configurations.annotationProcessor.get().extendsFrom(this)
    configurations.testAnnotationProcessor.get().extendsFrom(this)
}

dependencies {
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.4.0")
    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("org.freedesktop.dbus:dbus-java:2.7")

    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationImplementation("org.projectlombok:lombok:1.18.36")
}

// Task:
tasks.compileJava {
    options.encoding = "UTF-8"
}

// Publishing:
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "me.darragh"
            artifactId = "playing-api"
            version = project.version.toString()

            pom {
                name.set("playing-api")
                properties.set(mapOf(
                    "java.version" to "17",
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
                username = System.getenv("REPO_TOKEN")
                password = System.getenv("REPO_SECRET")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}