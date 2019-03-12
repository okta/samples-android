# Custom Sign In Example

This example shows you how to use the [Okta AppAuth Library][] with [Okta Java Authentication SDK] to authenticate a user within application and how to work with [Okta Authentication API] using [Okta Java Authentication SDK] library.
Authentication is done by getting sessionToken by [Okta Java Authentication SDK] and process authenticate using [Okta AppAuth Library] within application without a browser ([Chrome Custom Tabs] []). 
MFA (Multi Factor Authentication) and Recovery password are done by using [Okta Java Authentication SDK] library.

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
* (Optional) Configure MFA policy for user or app.
    * Configure MFA
        1. Navigate over to the Security tab, then select Multifactor from the dropdown menu.
        2. Select Factor Enrollment tab and Add Multifactor Policy
        3. In the Add Policy dialog, chosee which MFA you want and assign user groups. This will create MFA policy for user groups.
    * Enable MFA for user group in org
        1. Navigate over to the Security tab, then select Authentication from the dropdown menu.
        2. Select the Sign On tab and Add Rule to either your default or custom policy.
        3. In the Add Rule dialog, select Prompt for Factor and Every Time. This will prompt for MFA whenever a user is accessing your Okta organization. 
    * Enable MFA for app
        1. Navigate over to the Applications tab, then select Applications from the dropdown menu.
        2. Find your app and select it.
        3. Click on the Sign On tab, and scroll down toward the bottom to find the Sign On Policy section.
        4. In the dialog, under Actions -> Access - click the Prompt for factor checkbox and select Every sign on.

**Note:** *As with any Okta application, make sure you assign Users or Groups to the application. Otherwise, no one can use it.*

### Configuration

#### Setup configuration file 
Create a file called `okta_app_auth_config.json` in your application's `res/raw/` directory with
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
  "issuer_uri": "https://{yourOktaDomain}/oauth2/default"
}
```

**Note**: *To receive a **refresh_token**, you must include the `offline_access` scope.*
**Note**: `end_session_redirect_uri` is a mandatory parameter.

#### Update the URI Scheme and Base Url

You should set baseUrl of your Okta organization. Also [Okta AppAuth Library][] requires to specify a unique URI in order to redirect back to your application from a web browser. 
To do this, you must define a gradle manifest placeholder in your app's `build.gradle`:

```java
android {
    ...
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://{yourOktaDomain}\"")
        manifestPlaceholders = [
            hostName:"{yourOktaDomain}",
            "appAuthRedirectScheme": "com.lohika.android.test"
        ]
    }
}
```
Make sure this is consistent with the redirect URI Okta domain used in `okta_app_auth_config.json` . For example,
if your **Redirect URI** is `com.okta.example:/callback`, the **AppAuth Redirect Scheme** should be
`com.okta.example`.
if your **Base URL** is `https://okta.okta.com/oauth2/default`, the **Okta Domain** should be
`okta.okta.com`.

## Dependencies

Add the [Okta AppAuth Library][] dependency to your `build.gradle` file:
This library is responsible for exchange sessionToken on OpenID tokens(id, access, refresh) and persisting them
```bash
implementation 'com.okta.android:appauth-android:0.2.1'
```
Add the [Okta Java Authentication SDK][] dependency to your `build.gradle` file
This library is a convenience wrapper around [Okta Authentication API][].
```bash
implementation 'com.okta.authn.sdk:okta-authn-sdk-api:0.3.0'
runtimeOnly 'com.okta.authn.sdk:okta-authn-sdk-impl:0.3.0'
runtimeOnly 'com.okta.sdk:okta-sdk-okhttp:1.4.1'
```

## Running This Example

You can open this sample into Android Studio or build it using gradle.
```bash
cd custom-sign-in
./gradlew app:assembleRelease
```

**NativeSignInActivity.java** - implementation authentication process.
* With MFA
    * **NativeSignInWithMFAFragment.java** - implement authentication with MFA
        * **MfaCallFragment.java** - resolve MFA via call.
        * **MfaSMSFragment.java** - resolve MFA via SMS.
        * **MfaOktaVerifyCodeFragment.java** - resolve MFA via code in [Okta Verify][] application.
        * **MfaOktaVerifyPushFragment.java** - resolve MFA via Push in [Okta Verify][[] application
        * **MfaGoogleVerifyCodeFragment.java** - resolve MFA via Code in [Google Authenticator][]
* Without MFA
    * **NativeSignInFragment.java** - implement authentication without MFA

**PasswordRecoveryActivity.java** - implementation of recovery paswword
* **PasswordRecoveryFragment** - perform recovery password request
* **RecoveryQuestionFragment** - answer on security question
* **PasswordResetFragment** - reset password

**UserInfoActivity.java** - get user info, log out.

[Okta Authentication API]: https://developer.okta.com/docs/api/resources/authn.html
[Okta Java Authentication SDK]: https://github.com/okta/okta-auth-java
[Okta AppAuth Library]: https://github.com/okta/okta-sdk-appauth-android
[Chrome Custom Tabs]: https://developer.chrome.com/multidevice/android/customtabs
[Authorization Code Flow with PKCE]: https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce
[Google Authenticator]: https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2
[Okta Verify]: https://play.google.com/store/apps/details?id=com.okta.android.auth