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
        local("C:/Program Files/JetBrains/IntelliJ IDEA 2026.1")

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
            local(
                file("C:/Program Files/JetBrains/IntelliJ IDEA 2026.1")
            )
        }
    }
}