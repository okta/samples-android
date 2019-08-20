# Android Samples

This repository contains 2 different Android sample applications that show you how to authenticate to Okta account and how to work with the OKTA API in your Android application.

Please find the sample that fits your use-case from the table below.

| Sample | Description | Use-Case |
|--------|-------------|----------|
| [Browser Sign In](/browser-sign-in) | An application that show how to authenticate to Okta account via WebBrowser ([Chrome Custom Tabs][]) using OpenID protocol and get acccount detail info. | Native Android app. Authenticate to app via browser without creation any additional login form and using already authenticate session(if user do authentication in browser before). |
| [Custom Sign In](/custom-sign-in) | An application that show how to authenticate to Okta account within app without any browsers. Also it shows how verify MFA(Multi Factor Authentication) such as Call, SMS, OktaVerify(Push, Code), Google Authenticator. Demonstrating how to implement password recovery process, answering security question.  | Native Android app. Implementing whole authenticate process within application without other apps (like browser) |
| [Sign In Kotlin](/sign-in-kotlin) | A Kotlin example of authentication flows using the WebBrowser ([Chrome Custom Tabs][]) and custom sign-in. | Native Kotlin Android app. Authenticate via browser and custom sign-in using [Okta Authentication SDK](https://github.com/okta/okta-auth-java) |
| [TOTP Generator](/totp) | A sample application that generates TOTP tokens. | An application that shows how developer can build their own Google Authenticator clone for their brand. |

[Okta Authentication API]: https://developer.okta.com/docs/api/resources/authn.html
[Okta Java Authentication SDK]: https://github.com/okta/okta-auth-java
[Okta OIDC Library]: https://github.com/okta/okta-oidc-android
[Chrome Custom Tabs]: https://developer.chrome.com/multidevice/android/customtabs
[Authorization Code Flow with PKCE]: https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce
[Google Authenticator]: https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2
[Okta Verify]: https://play.google.com/store/apps/details?id=com.okta.android.auth