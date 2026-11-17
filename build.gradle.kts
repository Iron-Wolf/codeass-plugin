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
    // Main
    implementation(libs.gson) // Bundled into the plugin distribution
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
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
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    test {
        useJUnit()
        testLogging {
            showStandardStreams = true
            events("standardOut", "standardError", "failed")
        }
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
