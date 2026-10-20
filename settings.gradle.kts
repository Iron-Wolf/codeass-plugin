pluginManagement {
    repositories {
        maven("http://nexus.pic.services.prod/repository/gradle-public/") {
            isAllowInsecureProtocol = true
        }
        maven("http://nexus.pic.services.prod/repository/maven-public/") {
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "codeass-plugin"

