import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = "com.pravalika.springapiguard"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        if (System.getenv("INTELLIJ_LOCAL_PATH") != null) {
            local(System.getenv("INTELLIJ_LOCAL_PATH")!!)
        } else {
            intellijIdea("2026.1.5")
        }

        bundledPlugin("com.intellij.java")

        testFramework(
            TestFrameworkType.Platform
        )
    }
}

java {
    toolchain {
        languageVersion.set(
            JavaLanguageVersion.of(21)
        )
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            select {
                version = "2026.1"
            }
        }
    }
}
