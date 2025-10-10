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
| `SHIFT_SCHEDULE_PATH` | `PHP/shift_functions.php` | Relative path (resolved against `API_BASE_URL`) used to fetch the next shift. The Android client sends a `POST` request with `action=<SHIFT_FETCH_ACTION>` and `user_id=<staff id>`, so ensure your PHP handler accepts form posts. The Cindy's Bakeshop repo ships `shift_functions.php`; point this at that script (or a thin wrapper around it) unless your backend exposes the utilities somewhere else. |
| `SHIFT_ACTION_PATH` | `SHIFT_SCHEDULE_PATH` | Relative path used when starting a shift. Override if your read and write endpoints differ. |
| `SHIFT_FETCH_ACTION` | `get_shift_schedules` | The `action` form field sent when requesting the next scheduled shift. |
| `SHIFT_START_ACTION` | `start_shift` | The `action` form value posted when starting a shift. |
| `USER_PROFILE_PATH` | `PHP/user_api.php` | Relative path queried to look up the signed-in driver's account details. |
| `USER_PROFILE_ACTION` | `get_profile` | Action parameter passed when resolving the driver's profile. |
| `DEFAULT_STAFF_USER_ID` | `0` | Optional fallback `User_ID` to use when no Firebase login is available. |

When a driver signs in with Firebase, the **Status** screen automatically calls
`USER_PROFILE_PATH` with the configured `USER_PROFILE_ACTION` and the driver's
email to retrieve the numeric `user_id`. That identifier is then passed to the
shift endpoints so the correct assignments load without hardcoding anything in
the app. Only set `DEFAULT_STAFF_USER_ID` for manual testing on builds where no
Firebase session exists.

> **Tip:** If you see a *“Shift endpoint was not found (HTTP 404)”* message in
> the app, the configured `SHIFT_SCHEDULE_PATH` probably does not exist on your
> server. Deploy the `shift_functions.php` helper (or whichever wrapper exposes
> those functions over HTTP) from the
> [Cindy's Bakeshop repo](https://github.com/CloudMonkeyCoding/Cindys-Bakeshop/blob/main/PHP/shift_functions.php),
> or point the Gradle property at the script that serves your JSON shift data.

Declare the values inline while you build:

```sh
./gradlew assembleDebug \
  -PUSER_PROFILE_PATH=PHP/user_api.php \
  -PUSER_PROFILE_ACTION=get_profile \
  -PDEFAULT_STAFF_USER_ID=7 \
  -PSHIFT_SCHEDULE_PATH=PHP/shift_functions.php \
  -PSHIFT_FETCH_ACTION=get_shift_schedules
```

or add them to `gradle.properties` so Android Studio picks them up automatically:

```
USER_PROFILE_PATH=PHP/user_api.php
USER_PROFILE_ACTION=get_profile
DEFAULT_STAFF_USER_ID=7
SHIFT_SCHEDULE_PATH=PHP/shift_functions.php
SHIFT_ACTION_PATH=PHP/shift_functions.php
SHIFT_FETCH_ACTION=get_shift_schedules
SHIFT_START_ACTION=start_shift
```

The shift endpoint is expected to return JSON similar to the helper functions
in [`PHP/shift_functions.php`](https://github.com/CloudMonkeyCoding/Cindys-Bakeshop/blob/main/PHP/shift_functions.php).
On success the loader looks for a `shift` object (or the first element in a
`shifts`/`data` array) with fields such as `Shift_ID`, `Shift_Date`,
`Scheduled_Start`, `Scheduled_End`, `Actual_Start`, and `Status`. When the
`start_shift` action succeeds the script should respond with the updated shift
record or the app will issue a follow-up fetch to refresh the UI.

## Configuring delivery order lookups

The **Deliveries** tab now lists every order assigned to the signed-in driver
that has not been marked complete. Two additional Gradle properties control
which PHP script is queried and which action parameter is appended to the
request:

| Property | Default | Purpose |
| --- | --- | --- |
| `ORDER_LIST_PATH` | `PHP/order_api.php` | Relative path (resolved against `API_BASE_URL`) used to fetch the driver's orders. The client sends a `GET` request with `action=<ORDER_LIST_ACTION>` and `user_id=<staff id>`. Point this at the Cindy's Bakeshop `order_api.php` helper or a compatible endpoint that returns a JSON array of orders. |
| `ORDER_LIST_ACTION` | `list` | Action query parameter appended when requesting orders for the driver. |

Each order response should include fields such as `Order_ID`, `Status`,
`Item_Count`, `Item_Summary`, `Total_Amount`, `Order_Date`, `Fulfillment_Type`,
and optionally `Source`. The Android client filters out records whose status is
`Delivered`, `Completed`, or `Cancelled` so the screen only surfaces unfinished
work. If your back-end uses different status text, adjust the PHP response so
active orders return a distinct status string and the mobile app can continue
to display the correct queue.
