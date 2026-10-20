# Testing with IntelliJ Sandbox IDE

You do **not** need to manually build the `.zip` file or install it through Settings every time you test code changes.

The project uses the **IntelliJ Platform Gradle Plugin**, which includes a built-in sandbox IDE runner (`runIde`).

---

## What is the Sandbox IDE?

When you launch the sandbox:
1. Gradle compiles your main and automatically places it in a dedicated sandbox directory (`.intellijPlatform/sandbox/codeass-plugin/IU-2025.3.3/`).
2. An isolated instance of IntelliJ IDEA 2025.3.3 is launched with your main already installed and activated.
3. **Your real IntelliJ IDEA is completely untouched**:
   - The sandbox has its own separate `config`, `system`, and `plugins` directories.
   - Settings in your real IDE remain safe and unaffected.

---

## How to Run It

### Option 1: Directly inside IntelliJ IDEA (Recommended)
1. Open this project in **IntelliJ IDEA**.
2. Look at the top-right toolbar: the run configuration **`Run Plugin (Sandbox IDE)`** is automatically configured.
3. Click the green **Run (▶)** button (or press `Shift + F10`).
4. To debug with breakpoints, click the **Debug (🐞)** button (or press `Shift + F9`).
   - Breakpoints set in your Java code will trigger directly while using the sandbox IDE!

---

### Option 2: Using the Convenient Scripts
- **Double-click** `start-sandbox.bat` in Windows Explorer, or run in PowerShell / Command Prompt:
  ```cmd
  start-sandbox.bat
  ```
- To start with a suspended JVM listening on port `5005` for remote debugging:
  ```cmd
  start-sandbox-debug.bat
  ```

---

## Useful Sandbox Tips

- **Fast Iteration**: Whenever you change code, simply stop the sandbox IDE (or close its window) and click Run again. Gradle will incrementally recompile in ~1–2 seconds and launch the updated main immediately.
- **Resetting the Sandbox**: If you ever want to reset the sandbox configuration and start fresh:
  ```cmd
  start-sandbox.bat cleanSandbox
  ```
  or via Gradle:
  ```cmd
  .\gradlew.bat cleanSandbox
  ```
- **Sandbox Storage Location**:
  The isolated sandbox files are located at:
  `.intellijPlatform/sandbox/codeass-plugin/IU-2025.3.3/`
