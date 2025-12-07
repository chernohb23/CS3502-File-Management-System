# CS-3502 File Management System

A simple JavaFX application for browsing directories, viewing/editing text files, and performing basic file operations (create, rename, delete).

## Prerequisites

- Java Development Kit (JDK) 25 installed and on your PATH
  - Verify: `java -version` should show version 25
- Maven 3.9+ installed
  - Alternatively, use the included Maven Wrapper (`./mvnw` on Unix/macOS, `mvnw.cmd` on Windows)

## Quick Start

From the project root:

- Using Maven (requires Maven installed):
  - macOS/Linux:
    - Build: `mvn clean install`
    - Run: `mvn javafx:run`
  - Windows:
    - Build: `mvn clean install`
    - Run: `mvn javafx:run`

- Using the Maven Wrapper (no global Maven needed):
  - macOS/Linux:
    - Build: `./mvnw clean install`
    - Run: `./mvnw javafx:run`
  - Windows:
    - Build: `mvnw.cmd clean install`
    - Run: `mvnw.cmd javafx:run`

This launches the JavaFX application in a new window.

## Build Artifacts

- Package (JAR): `mvn clean package`
  - The JAR will be in `target/`.
  - Note: Running the packaged JAR directly typically requires providing JavaFX modules on the module path. For the simplest experience, prefer `mvn javafx:run`.

- Optional: Create a self-contained runtime image (jlink):
  - `mvn clean javafx:jlink`
  - Launch the app from the generated image:
    - macOS/Linux: `target/app/bin/app`
    - Windows: `target\app\bin\app.bat`

## Running Tests

- `mvn test`

## Troubleshooting

- “Could not find or load main class …” when running:
  - Ensure you run via `mvn javafx:run` (or wrapper) from the project root.
  - If you modified the application’s main class name/package, ensure the Maven JavaFX plugin’s `mainClass` configuration matches the fully qualified name and module.

- Java version mismatch:
  - Ensure you’re using JDK 25. If you have multiple JDKs installed, set `JAVA_HOME` to your JDK 25 installation and ensure your PATH uses it first.

## Useful Commands (Summary)

- Build: `mvn clean install`
- Run: `mvn javafx:run`
- Tests: `mvn test`
- Clean: `mvn clean`
- With wrapper (no Maven install): replace `mvn` with `./mvnw` (Unix/macOS) or `mvnw.cmd` (Windows).
