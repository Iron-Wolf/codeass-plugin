import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.intellijPlatform)
}

group = "codeass"
version = "1.0.1"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    maven("http://nexus.pic.services.prod/repository/maven-public/") {
        isAllowInsecureProtocol = true
    }
    maven("http://nexus.pic.services.prod/repository/gradle-public/") {
        isAllowInsecureProtocol = true
    }
    intellijPlatform {
        localPlatformArtifacts()
    }
}

dependencies {
    intellijPlatform {
        // Uses YOUR locally extracted IU-262.10968.63 ZIP (no download, no Toolbox needed)
        local(providers.gradleProperty("platformLocalPath"))

        // Needed to compile/run IntelliJ platform tests (optional, disabled when offline/behind proxy)
        // testFramework(TestFrameworkType.Platform)
    }
    // Lombok configuration
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Bundled into the plugin distribution automatically
    implementation(libs.gson)

    testImplementation(libs.junit)
}

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
    pluginVerification {
        ides {
            local(providers.gradleProperty("platformLocalPath"))
        }
    }
}

tasks {
    test {
        useJUnit()
    }
    runIde {
        // Sandbox IDE gets its own config/plugins/system dirs, your real IDE is untouched
        jvmArgs("-Xmx2g")
    }
    // Désactive le dump des releases externes (évite UnknownHostException: jb.gg en réseau fermé)
    named("dumpProductsReleases") {
        enabled = false
    }
}
