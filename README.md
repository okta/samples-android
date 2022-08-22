# Android Samples

> Note: These samples have not been updated to use the new [Okta Mobile SDK](https://github.com/okta/okta-mobile-kotlin), but you can visit the [quick start guide](https://developer.okta.com/docs/guides/sign-into-mobile-app-redirect/android/main/) to learn how to use it in your own applications.

This repository contains 3 different Android sample applications that show you how to authenticate to Okta account and how to work with the OKTA API in your Android application.

Please find the sample that fits your use-case from the table below.

| Sample | Description | Use-Case |
|--------|-------------|----------|
| [Browser Sign In](/browser-sign-in) | An application that show how to authenticate to Okta account via WebBrowser ([Chrome Custom Tabs][]) using OpenID protocol and get acccount detail info. | Native Android app. Authenticate to app via browser without creation any additional login form and using already authenticate session(if user do authentication in browser before). |
| [TOTP Generator](/totp) | A sample application that generates TOTP tokens. | An application that shows how developer can build their own Google Authenticator clone for their brand. |

## MinSDK Requirements
The [Okta OIDC Android SDK](https://github.com/okta/okta-oidc-android#requirements) supports Android 5.0 (lolipop) and above (minSdkVersion >= 21).

[Okta Authentication API]: https://developer.okta.com/docs/api/resources/authn.html
[Okta Java Authentication SDK]: https://github.com/okta/okta-auth-java
[Okta OIDC Library]: https://github.com/okta/okta-oidc-android
[Chrome Custom Tabs]: https://developer.chrome.com/multidevice/android/customtabs
[Authorization Code Flow with PKCE]: https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce
[Google Authenticator]: https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2
[Okta Verify]: https://play.google.com/store/apps/details?id=com.okta.android.auth
