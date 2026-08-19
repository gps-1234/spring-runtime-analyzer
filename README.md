# API Change Guard

**API Change Guard** is an IntelliJ IDEA plugin that detects changes in Spring Boot REST APIs by comparing the current API surface with the version stored at the Git HEAD.

It helps developers identify potentially breaking API changes early, directly from IntelliJ IDEA, before those changes reach downstream consumers.

## Why API Change Guard?

Changing a REST API can unintentionally break clients, integrations, or other services that depend on it.

API Change Guard provides a lightweight way to review API changes during development by comparing the current Spring API definitions against the previous Git version.

Instead of manually reviewing controller changes or switching between Git diffs and source files, developers can inspect detected API changes directly inside IntelliJ IDEA.

## Features

- 🔍 **Spring REST API discovery** — Scans Spring source code to identify REST endpoints.
- 🔄 **Git-based API comparison** — Compares the current API surface with the version available at Git HEAD.
- ⚠️ **API change detection** — Identifies changes between the previous and current API definitions.
- 📋 **Structured change information** — Represents detected changes using endpoint, parameter, and API change models.
- 🖥️ **IntelliJ IDEA integration** — Provides the analysis through an IntelliJ IDEA tool window.
- 🧪 **Automated tests** — Includes unit tests for the API change analysis logic.

## How It Works

API Change Guard follows a simple comparison workflow:

```
   Current Project              Git HEAD
         │                          │
         ▼                          ▼
  Spring API Scanner       Previous API Scanner
         │                          │
         ▼                          ▼
  Current API Model        Previous API Model
         │                          │
         └────────────┬─────────────┘
                       ▼
             API Change Analyzer
                       │
                       ▼
            Detected API Changes
                       │
                       ▼
            IntelliJ Tool Window
```

The plugin:

1. Scans the current project for Spring REST API endpoints.
2. Retrieves the corresponding source state from Git HEAD.
3. Scans the previous version of the API.
4. Compares the two API representations.
5. Reports detected API changes inside IntelliJ IDEA.

## Example

Suppose the previous version contains:

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    // ...
}
```

and the current version changes the endpoint to:

```java
@GetMapping("/users/{userId}")
public User getUser(@PathVariable Long userId) {
    // ...
}
```

API Change Guard can detect the difference in the API definition and surface the change for review.

## Supported API Concepts

API Change Guard currently focuses on Spring-based REST APIs and models information such as:

- HTTP endpoints
- HTTP methods
- Endpoint paths
- Request parameters
- API parameter information
- Changes between Git versions

Support for additional Spring API constructs may be added in future releases.

## Installation

### From JetBrains Marketplace

Once published, API Change Guard can be installed directly from IntelliJ IDEA:

1. Open **Settings / Preferences**.
2. Select **Plugins**.
3. Open the **Marketplace** tab.
4. Search for **API Change Guard**.
5. Select **Install**.
6. Restart IntelliJ IDEA if prompted.

### From a Plugin ZIP

For development or testing, build the plugin locally:

```bash
./gradlew buildPlugin
```

The generated plugin ZIP will be available under:

```
build/distributions/
```

You can then install the ZIP through **Settings → Plugins → ⚙ → Install Plugin from Disk**.

## Usage

1. Open a Spring Boot project in IntelliJ IDEA.
2. Make sure the project is managed by Git.
3. Make changes to your Spring REST APIs.
4. Open the **API Change Guard** tool window.
5. Run the API analysis.
6. Review the detected differences between the current project and Git HEAD.

For the most accurate comparison, make sure the project has a valid Git HEAD containing the previous version of the source code.

## Requirements

- IntelliJ IDEA
- Git
- A Spring-based project containing REST APIs

## Development

Clone the repository:

```bash
git clone https://github.com/gps-1234/spring-runtime-analyzer.git
cd spring-runtime-analyzer
```

Build the plugin:

```bash
./gradlew buildPlugin
```

Run the plugin in a development IntelliJ IDEA instance:

```bash
./gradlew runIde
```

Run tests:

```bash
./gradlew check
```

Verify the plugin:

```bash
./gradlew verifyPlugin
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/pravalika/springapiguard/
│   │       ├── analyzer/
│   │       │   ├── ApiChange.java
│   │       │   ├── ApiChangeAnalyzer.java
│   │       │   ├── ApiEndpoint.java
│   │       │   ├── ApiParameter.java
│   │       │   ├── GitHeadSnapshot.java
│   │       │   ├── GitHeadSpringApiScanner.java
│   │       │   └── SpringApiScanner.java
│   │       └── toolWindow/
│   │           └── ApiChangeGuardToolWindowFactory.java
│   │
│   └── resources/
│       └── META-INF/
│           ├── plugin.xml
│           └── pluginIcon.svg
│
└── test/
    └── java/
        └── com/pravalika/springapiguard/
            └── analyzer/
                └── ApiChangeAnalyzerTest.java
```

## Technology Stack

- **Java**
- **IntelliJ Platform SDK**
- **IntelliJ Platform Gradle Plugin**
- **Spring Boot / Spring Web API conventions**
- **Git**
- **Gradle**
- **JUnit**

## Screenshots

Screenshots demonstrating API Change Guard in IntelliJ IDEA will be added here.

### API Change Analysis

*Add screenshot showing the API Change Guard tool window and detected changes.*

### API Change Details

*Add screenshot showing the details of detected API changes.*

## Roadmap

Potential future improvements include:

- More comprehensive Spring annotation support
- More detailed breaking-change classification
- Improved API change visualization
- Additional parameter and request-body analysis
- Better support for complex Spring MVC mappings
- Configuration options for API comparison behavior
- Integration with CI/CD workflows

## Contributing

Contributions, bug reports, feature requests, and suggestions are welcome.

Before submitting a pull request:

1. Build the plugin: `./gradlew buildPlugin`
2. Run the tests: `./gradlew check`
3. Verify the plugin: `./gradlew verifyPlugin`

Please keep changes focused and include tests for new analysis behavior where appropriate.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author

**Pravalika Satya**

If you find API Change Guard useful, consider giving the repository a ⭐.