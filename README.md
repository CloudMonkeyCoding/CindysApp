# CindysApp

## Building the project locally

The Android Gradle plugin expects to know where your local Android SDK is
installed. Before running any Gradle task, copy `local.properties.example` to
`local.properties` and update the `sdk.dir` entry so it points at the SDK on
your machine. A few common locations are documented in the template file.

Once the SDK path is configured you can build the app from the command line with:

```sh
./gradlew assembleDebug
```

If you prefer Android Studio, open the project directory in the IDE and it will
reuse the same `local.properties` configuration.