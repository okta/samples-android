# TOTP Generator Example

This example shows you how to build your own Google Authenticator clone for your brand. Users can
scan a QR image (during enrollment in the Okta TOTP
factor: https://developer.okta.com/docs/api/resources/authn/#enroll-okta-verify-totp-factor) and
then generate codes that can be used to verify the TOTP factor.

## Prerequisites

Before running this sample, you will need the following:

* An Okta Developer Account, you can sign up for one at https://developer.okta.com/signup/.
* An Okta Application, configured for Android client. This is done from the Okta Developer Console
  and you can find instructions [here][OIDC Native Android Application Setup Instructions]. When
  following the wizard, use the default properties. They are designed to work with our sample
  applications.
* Setup MFA for your Okta Developer Account, enable Google Authenticator factor.

## Running This Example

To run this application, you first need to clone this repo and enter the root project directory:

```bash
git clone https://github.com/okta/samples-android.git
cd samples-android/
```

Now you can build and run the application as follows:

```bash
./gradlew :totp:assembleDebug
./gradlew :totp:installDebug
```

Clicking the **Add** button will allow you to setup TOTP generator for your account. You can do this
either by scanning QR image or by entering secret key manually. Once you do this, TOTP will be
refreshed automatically.

[OIDC Native Android Application Setup Instructions]: https://developer.okta.com/quickstart/#/android/nodejs/express
