# Build & Packaging Summary - `codeass-plugin`

The first version of the project was initialized with a throwaway prompt.  
This document summarizes the changes applied to have a **true** first working build.

---

## 1. Context and Objectives

- **Target IDE**: IntelliJ IDEA 2025.3.3 (`IU-253.30387.88`) located at:
  `%userprofile%\AppData\Local\Programs\idea-2025.3.3.win`
- **Java Version**: Java 21 (JBR 21 bundled with IDEA 2025.3.3 and local JDK 21).
- **Environment**: Offline corporate network (no direct access to external Maven repositories, Gradle Plugin Portal, or GitHub releases).

---

## 2. Issues Encountered & Root Causes

### A. Java Bytecode Incompatibility with IDEA 2026.2.3
- **Issue**: Attempting to compile against `idea-2026.2.3.win` produced class file format version 69 errors (`unsupported class file major version 69`), requiring Java 25.
- **Root Cause**: `idea-2026.2.3.win` uses early-access Java 25 bytecode. The local installed JDKs are Java 21.
- **Resolution**: Switched target platform to `idea-2025.3.3.win`, which uses Java 21 (bytecode major version 65).

### B. StatusBarWidget API Evolution in IntelliJ Platform 2025.3
- **Issue**: Compilation failure in `MyStatusBarWidget.java` with `@Override public String getId()`.
- **Root Cause**: In IntelliJ Platform 2025.3, the `StatusBarWidget` interface changed `getId()` to `ID()`.
- **Resolution**: Renamed method implementation and internal references to `@NotNull @Override public String ID()`.

### C. Network Dependency Hangs & Failures in Offline Environment
1. **Plugin Update Check**:
   - `InitializeIntelliJPlatformPluginTask` queried GitHub releases for newer main versions, hanging or throwing exceptions due to network timeouts.
   - **Resolution**: Added `org.jetbrains.intellij.platform.selfUpdateCheck=false` to `gradle.properties`.
2. **Code Instrumentation Ant Task Download**:
   - `instrumentCode` task attempted to download `com.jetbrains.intellij.java:java-compiler-ant-tasks` from remote maven repositories.
   - **Resolution**: Disabled code instrumentation via `instrumentCode = false` in `build.gradle.kts`.
3. **Searchable Options Headless IDE Execution**:
   - `buildSearchableOptions` launched a headless IDE instance.
   - **Resolution**: Disabled via `buildSearchableOptions = false` in `build.gradle.kts`.

---

## 3. Configuration & Code Changes

### `gradle.properties`
```properties
# Local IntelliJ IDEA 2025.3.3 installation
platformLocalPath=<PATH>/idea-2025.3.3.win

# Target build range (2025.3)
pluginSinceBuild=253
pluginUntilBuild=253.*

# Java 21 environment setup
org.gradle.java.home=<PATH>/idea-2025.3.3.win/jbr
org.gradle.java.installations.auto-download=false
org.gradle.java.installations.auto-detect=false
org.gradle.java.installations.paths=<PATH>/jdk-21.0.12.1+1
org.gradle.jvmargs=-Xmx2048m
org.gradle.caching=true
org.gradle.configuration-cache=false
kotlin.stdlib.default.dependency=false

# Disable external update checks for offline environment
org.jetbrains.intellij.platform.selfUpdateCheck=false
```

### `build.gradle.kts`
- Configured Java toolchain:
```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```
- Configured `intellijPlatform` extension:
```kotlin
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
```

### `statusBar`
- Updated widget identifier to match IntelliJ 2025.3 API:
```java
@NotNull
@Override
public String ID() {
    return "MonWidgetUniqueId";
}
```
- Updated widget update call:
```java
main.updateWidget(ID());
```

---

## 4. Build & Verification

### Build Command
```powershell
.\gradlew.bat clean buildPlugin --console=plain
```
**Result**: `BUILD SUCCESSFUL` (tasks completed cleanly in 1–4 seconds with caching enabled).

### Bytecode Verification
- Checked compiled class `iw/MyStatusBarWidget.class` using `javap`:
  - `major version: 65` (Java 21).

### Generated Package
- **Archive Path**: `build/distributions/codeass-plugin-1.0.0.zip` (257 KB)
- **Archive Contents**:
  - `codeass-plugin/lib/codeass-plugin-1.0.0.jar`
    - Contains compiled Java 21 `.class` files.
    - Contains `META-INF/plugin.xml` with `since-build="253"` and `until-build="253.*"`.
  - `codeass-plugin/lib/gson-2.10.1.jar`

---

## 5. Installation Instructions

1. Open **IntelliJ IDEA 2025.3.3**.
2. Go to **File** > **Settings** (or `Ctrl+Alt+S`) > **Plugins**.
3. Click the gear icon (`⚙️`) next to the search bar.
4. Select **Install Plugin from Disk...**.
5. Select the zip file: `codeass-plugin-1.0.0.zip`
6. Restart the IDE when prompted.
