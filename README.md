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

## Pointing the app at your own server

The app now looks for a delivery back-end at `https://evotech.slarenasitsolutions.com/`
by default. You can change the URL without editing the source code by providing
an `API_BASE_URL` Gradle property when you build:

```sh
./gradlew assembleDebug -PAPI_BASE_URL=https://your-server.example.com/
```

If you prefer to keep the value locally, add the same property to your
`gradle.properties` or `~/.gradle/gradle.properties`:

```
API_BASE_URL=https://your-server.example.com/
```

At runtime the **Status** screen pings the `API_BASE_URL` (optionally combined
with the relative path in `res/values/strings.xml#server_health_path`) to show
whether the server is reachable. Update that string if your health-check lives
under a different endpoint, for example `api/ping`.
