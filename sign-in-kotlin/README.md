# Kotlin Sign In Example

This example shows you how to use the [Okta OIDC Library] to authenticate a user using Kotlin. It includes examples on authentication using [Chrome Custom Tabs] or a native interface that calls [Okta Authentication API] directly.

## Prerequisites

Before running this sample, you will need the following:

* An Okta Developer Account, you can sign up for one at <https://developer.okta.com/signup/>.
* An Okta Application, configured for Mobile app.
    1. After login, from the Admin dashboard, navigate to **Applications**&rarr;**Add Application**
    2. Choose **Native** as the platform
    3. Populate your new Native OpenID Connect application with values similar to:

        | Setting              | Value                                               |
        | -------------------- | --------------------------------------------------- |
        | Application Name     | MyApp *(must be unique)*        |
        | Login URI            | com.okta.example:/callback                          |
        | End Session URI      | com.okta.example:/logoutCallback                    |
        | Allowed grant types  | Authorization Code, Refresh Token *(recommended)*   |

    4. Click **Finish** to redirect back to the *General Settings* of your application.
    5. Copy the **Client ID**, as it will be needed for the client configuration.
    6. Get your issuer, which is a combination of your Org URL (found in the upper right of the console home page) . For example, <https://dev-1234.oktapreview.com.>

**Note:** *As with any Okta application, make sure you assign Users or Groups to the application. Otherwise, no one can use it.*

### Configuration

#### Setup configuration file

Create a file called `config.json` in your application's `res/raw/` directory with
the following contents:

```json
{
  "client_id": "{clientId}",
  "redirect_uri": "{redirectUri}",
  "end_session_redirect_uri": "{endSessionUri}",
  "scopes": [
    "openid",
    "profile",
    "offline_access"
  ],
  "discovery_uri": "https://{yourOktaDomain}"
}
```

**Note**: *To receive a **refresh_token**, you must include the `offline_access` scope.*
**Note**: `end_session_redirect_uri` is a mandatory parameter.

#### Update the URI Scheme

In order to redirect back to your application from a web browser, you must specify a unique URI to
your app. To do this, you must define a gradle manifest placeholder in your app's `build.gradle`:

```java
android.defaultConfig.manifestPlaceholders = [
    "appAuthRedirectScheme": "com.okta.example"
]
```

Make sure this is consistent with the redirect URI used in `config.json`. For example,
if your **Redirect URI** is `com.okta.example:/callback`, the **AppAuth Redirect Scheme** should be
`com.okta.example`.

#### Native sign in

Signing in using a native interface requires the `AuthenticationClient` to be setup with a `Org URL`(found in the upper right of the console home page). You can do this by adding the following to your `local.properties` file:

```bash
authn.orgUrl="https://{yourOrlUrl}"
```

Gradle build will pick up the property and set it in `BuildConfig`. But you can hard code the Org URL in code:

```java
authenticationClient = AuthenticationClients.builder()
  .setOrgUrl(BuildConfig.ORG_URL)
  .build()
```

**Note: Native sign in sample does not handle MFA use cases.**

## Dependencies

This sample use [Okta OIDC Library] dependency in `build.gradle` file:

```bash
implementation 'com.okta.android:oidc-androidx:1.0.2'
```

## Running This Example

You can open this sample into Android Studio or build it using gradle.

```bash
cd sign-in-kotlin
./gradlew app:assembleRelease
```

To switch between native interface or chrome custom tab go to the settings menu and select the desired mode. If running on the emulator you should disable `hardware keystore`.

[Okta Authentication API]: https://developer.okta.com/docs/api/resources/authn.html
[Okta Java Authentication SDK]: https://github.com/okta/okta-auth-java
[Okta OIDC Library]: https://github.com/okta/okta-oidc-android
[Chrome Custom Tabs]: https://developer.chrome.com/multidevice/android/customtabs
[Authorization Code Flow with PKCE]: https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce
