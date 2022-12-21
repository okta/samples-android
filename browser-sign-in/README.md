# Browser Sign In Example

This example shows you how to use [Okta Mobile Kotlin][] to authenticate a user. Authentication is done with the browser [Chrome Custom Tabs][], which open Okta login web-page. After the user authenticates in browser, they are redirected back to the application and exchanging the received code for tokens and persist them.

## Prerequisites

Before running this sample, you will need the following:

* An Okta Developer Account, you can sign up for one at https://developer.okta.com/signup/.
* An Okta Application, configured for Mobile app.
    1. After login, from the Admin dashboard, navigate to **Applications**&rarr;**Add Application**
    2. Choose **Native** as the platform
    3. Populate your new Native OpenID Connect application with values similar to:

        | Setting              | Value                                               |
        | -------------------- | --------------------------------------------------- |
        | Application Name     | Native OpenId Connect App *(must be unique)*        |
        | Login URI            | com.okta.example:/callback                          |
        | End Session URI      | com.okta.example:/logoutCallback                    |
        | Allowed grant types  | Authorization Code, Refresh Token *(recommended)*   |

    4. Click **Finish** to redirect back to the *General Settings* of your application.
    5. Copy the **Client ID**, as it will be needed for the client configuration.
    6. Get your issuer, which is a combination of your Org URL (found in the upper right of the console home page) and /oauth2/default. For example, https://dev-1234.oktapreview.com/oauth2/default.

**Note:** *As with any Okta application, make sure you assign Users or Groups to the application. Otherwise, no one can use it.*

### Configuration

#### Update configuration file
Update `okta.properties` in your application directory with the following contents:

```properties
issuer=https://${yourOktaDomain}/oauth2/default
clientId=${clientId}
signInRedirectUri=${redirectUri}
signOutRedirectUri=${endSessionUri}
```

**Notes:**
- `discovery_uri` can be customized for specific authorization servers. See [Discovery URI Guidance](https://github.com/okta/okta-oidc-android#discovery-uri-guidance) for more info.
- To receive a **refresh_token**, you must include the `offline_access` scope.
- `end_session_redirect_uri` is a mandatory parameter.

## Dependencies

This sample uses [AuthFoundationBootstrap][] and [WebAuthenticationUI][] from [Okta Mobile Kotlin][]

## Running This Example

You can open this sample into Android Studio or build it using gradle.
```bash
cd browser-sign-in
./gradlew app:assembleRelease
```

[Chrome Custom Tabs]: https://developer.chrome.com/multidevice/android/customtabs
[Authorization Code Flow with PKCE]: https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce
[Google Authenticator]: https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2
[Okta Verify]: https://play.google.com/store/apps/details?id=com.okta.android.auth
[AuthFoundationBootstrap]: https://github.com/okta/okta-mobile-kotlin/tree/master/auth-foundation-bootstrap
[WebAuthenticationUI]: https://github.com/okta/okta-mobile-kotlin/tree/master/web-authentication-ui
[Okta Mobile Kotlin]: https://github.com/okta/okta-mobile-kotlin
