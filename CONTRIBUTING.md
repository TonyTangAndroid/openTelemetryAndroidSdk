# Contributing

Welcome to OpenTelemetry Android repository!

Before you start - see OpenTelemetry general
[contributing](https://github.com/open-telemetry/community/blob/main/CONTRIBUTING.md)
requirements and recommendations.

Make sure to review the projects [license](LICENSE) and sign the
[CNCF CLA](https://identity.linuxfoundation.org/projects/cncf). A signed CLA will be enforced by an
automatic check once you submit a PR, but you can also sign it after opening your PR.

## Requirements

Java 17 or higher is required to build the projects in this repository.
The built artifacts can be used with Android API Level 21 and higher.
API levels 21 to 25 require desugaring of the core library.

## Building opentelemetry-android

1. Clone the repository
```
git clone https://github.com/open-telemetry/opentelemetry-android.git
cd opentelemetry-android
```

2. To build the android artifact, run the gradle wrapper with `assemble`:
```
./gradlew assemble
```

The output artifacts will be in `instrumentation/build/outputs/`.

3. To run the tests and code checks:
```
./gradlew check
```

## Code Conventions
