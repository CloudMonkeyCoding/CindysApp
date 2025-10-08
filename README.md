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

For Cindy's Bakeshop the public PHP endpoints documented at
<https://github.com/CloudMonkeyCoding/Cindys-Bakeshop> are hosted on
`https://evotech.slarenasitsolutions.com/`. The default configuration points
the health check at `PHP/notification_api.php`, which returns a 200 response
even without query parameters. If you expose a different script for
connectivity testing, override `server_health_path` accordingly or pass a
custom `API_BASE_URL` when building.

## Configuring shift scheduling integrations

The **Status** screen now loads the upcoming shift for a staff member and lets
them clock in by POSTing to the PHP utilities in the Cindy's Bakeshop repo. A
few new Gradle properties make the endpoints and actions configurable without
modifying Java sources:

| Property | Default | Purpose |
| --- | --- | --- |
| `SHIFT_SCHEDULE_PATH` | `PHP/shift_actions.php` | Relative path (resolved against `API_BASE_URL`) used to fetch the next shift. |
| `SHIFT_ACTION_PATH` | `SHIFT_SCHEDULE_PATH` | Relative path used when starting a shift. Override if your read and write endpoints differ. |
| `SHIFT_FETCH_ACTION` | `next_shift` | The `action` query parameter sent when requesting the next scheduled shift. |
| `SHIFT_START_ACTION` | `start_shift` | The `action` form value posted when starting a shift. |
| `DEFAULT_STAFF_USER_ID` | `0` | The numeric `User_ID` whose shift will be shown. Set this to a valid staff user in your database. |

Declare the values inline while you build:

```sh
./gradlew assembleDebug \
  -PDEFAULT_STAFF_USER_ID=7 \
  -PSHIFT_SCHEDULE_PATH=PHP/shift_actions.php \
  -PSHIFT_FETCH_ACTION=next_shift
```

or add them to `gradle.properties` so Android Studio picks them up automatically:

```
DEFAULT_STAFF_USER_ID=7
SHIFT_SCHEDULE_PATH=PHP/shift_actions.php
SHIFT_ACTION_PATH=PHP/shift_actions.php
SHIFT_FETCH_ACTION=next_shift
SHIFT_START_ACTION=start_shift
```

The shift endpoint is expected to return JSON similar to the helper functions
in [`PHP/shift_functions.php`](https://github.com/CloudMonkeyCoding/Cindys-Bakeshop/blob/main/PHP/shift_functions.php).
On success the loader looks for a `shift` object (or the first element in a
`shifts`/`data` array) with fields such as `Shift_ID`, `Shift_Date`,
`Scheduled_Start`, `Scheduled_End`, `Actual_Start`, and `Status`. When the
`start_shift` action succeeds the script should respond with the updated shift
record or the app will issue a follow-up fetch to refresh the UI.
