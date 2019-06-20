/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.android.samples.browser_sign_in.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.fragment.app.FragmentActivity;

import com.okta.android.samples.browser_sign_in.R;
import com.okta.oidc.storage.security.EncryptionManager;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;

import static android.app.Activity.RESULT_OK;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.P;

@TargetApi(M)
public class SmartLockHelper {
    private static final int REQUEST_CODE_CREDENTIALS = 100;
    private static final String FINGERPRINT_DIALOG_TAG = "FINGERPRINT_DIALOG_TAG";

    private FingerprintDialog mFingerprintDialog;
    private FingerprintDialogCallbacks credentialsCallback;

    public SmartLockHelper() {
        mFingerprintDialog = FingerprintDialog.newInstance();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREDENTIALS) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                if (credentialsCallback != null) {
                    credentialsCallback.onFingerprintSuccess(null);
                }
            } else {
                credentialsCallback.onFingerprintCancel();
            }
            credentialsCallback = null;
        }
    }

    public void showSmartLockChooseDialog(FragmentActivity activity, FingerprintDialogCallbacks callback, Cipher cipher) {
        String[] types = new String[]{activity.getString(R.string.fingerprint_type), activity.getString(R.string.unlock_screen_type)};


        AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.select_smartlock_type_message)
                .setSingleChoiceItems(types, -1, (dialog, which) -> {
                    dialog.dismiss();
                    if (!SmartLockHelper.isKeyguardSecure(activity)) {
                        activity.runOnUiThread(() ->
                                Toast.makeText(
                                        activity,
                                        activity.getString(R.string.setup_lockscreen_info_msg),
                                        Toast.LENGTH_LONG).show());
                        callback.onFingerprintCancel();
                        return;
                    }

                    if (activity.getString(R.string.fingerprint_type).equalsIgnoreCase(types[which])) {
                        if (!SmartLockHelper.isHardwareSupported(activity)) {
                            activity.runOnUiThread(() ->
                                    Toast.makeText(
                                            activity,
                                            activity.getString(R.string.hardware_not_support_fingerprint),
                                            Toast.LENGTH_LONG).show());
                            callback.onFingerprintCancel();
                            return;
                        }
                        if (!SmartLockHelper.isFingerprintAvailable(activity)) {
                            activity.runOnUiThread(() ->
                                    Toast.makeText(activity,
                                            activity.getString(R.string.fingerprint_not_enrolled),
                                            Toast.LENGTH_LONG).show());
                            callback.onFingerprintCancel();
                            return;
                        }

                        showBiometricPromptCompat(activity, callback);

                        //TODO: Remove after testing androidx.biometric:biometric lib
                        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        //    showBiometricPrompt(activity, callback, cipher);
                        //} else {
                        //    showFingerprint(activity, callback, cipher);
                        //}
                    } else if (activity.getString(R.string.unlock_screen_type).equalsIgnoreCase(types[which])) {
                        credentialsCallback = callback;
                        showConfirmCredentials(activity);
                    }
                })
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).create();

        alertDialog.show();
    }

    @TargetApi(LOLLIPOP)
    private void showConfirmCredentials(Activity activity) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.unlock_screen_title), "");
        if (intent != null) {
            activity.startActivityForResult(intent, REQUEST_CODE_CREDENTIALS);
        }
    }

    @TargetApi(P)
    @Deprecated
    private void showBiometricPrompt(FragmentActivity activity, FingerprintDialogCallbacks callback, Cipher cipher) {
        CancellationSignal mCancellationSignal = new CancellationSignal();
        BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(activity)
                .setTitle(activity.getString(R.string.fingerprint_alert_title))
                .setNegativeButton(activity.getString(R.string.cancel), activity.getMainExecutor(), (dialogInterface, i) -> {
                    callback.onFingerprintCancel();
                })
                .build();

        BiometricPrompt.AuthenticationCallback authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onFingerprintCancel();
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                callback.onFingerprintCancel();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                if (result.getCryptoObject() != null) {
                    callback.onFingerprintSuccess(result.getCryptoObject().getCipher());
                } else {
                    callback.onFingerprintSuccess(null);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onFingerprintCancel();
            }
        };

        biometricPrompt.authenticate(mCancellationSignal, activity.getMainExecutor(), authenticationCallback);
    }

    @Deprecated
    @TargetApi(M)
    private void showFingerprint(Activity activity, FingerprintDialogCallbacks callback, Cipher cipher) {
        if (mFingerprintDialog != null && activity.getFragmentManager().findFragmentByTag(FINGERPRINT_DIALOG_TAG) == null) {
            FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
            mFingerprintDialog.init(cryptoObject, new FingerprintDialog.FingerprintDialogCallbacks() {
                @Override
                public void onFingerprintSuccess() {
                    callback.onFingerprintSuccess(null);
                }

                @Override
                public void onFingerprintError(String error) {
                    callback.onFingerprintError(error);
                }

                @Override
                public void onFingerprintCancel() {
                    callback.onFingerprintCancel();
                }
            });
            mFingerprintDialog.show(activity.getFragmentManager(), FINGERPRINT_DIALOG_TAG);
        }
    }

    private void showBiometricPromptCompat(FragmentActivity activity, FingerprintDialogCallbacks callback) {
        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.fingerprint_alert_title))
                .setNegativeButtonText(activity.getString(R.string.cancel))
                .build();

        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(activity, Executors.newSingleThreadExecutor(), new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    callback.onFingerprintCancel();
                } else {
                    callback.onFingerprintError(errString.toString());
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onFingerprintSuccess(null);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                activity.runOnUiThread(() -> Toast.makeText(activity, "Fingerprint not recognized. Try again", Toast.LENGTH_SHORT).show());

            }
        });
        biometricPrompt.authenticate(promptInfo);
    }

    public static boolean isHardwareSupported(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isGrantedFingerprintPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED;
            // In some reason FingerprintManagerCompat return false when you check
            // isHardwareDetected on some devices. Better to use FingerprintManger to check it.
            boolean isSupportedByDevice = context.getSystemService(FingerprintManager.class).isHardwareDetected();
            return isGrantedFingerprintPermission && isSupportedByDevice;
        } else {
            return false;
        }
    }

    public static boolean isKeyguardSecure(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public static boolean isFingerprintAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getSystemService(FingerprintManager.class).hasEnrolledFingerprints();
        } else {
            return false;
        }
    }

    public interface FingerprintDialogCallbacks {
        void onFingerprintSuccess(Cipher cipher);

        void onFingerprintError(String error);

        void onFingerprintCancel();
    }

    public static abstract class FingerprintCallback implements FingerprintDialogCallbacks {
        private EncryptionManager encryptionManager;
        private WeakReference<Activity> mActivity;

        public FingerprintCallback(Activity activity, EncryptionManager encryptionManager) {
            this.mActivity = new WeakReference<>(activity);
            this.encryptionManager = encryptionManager;
        }

        protected abstract void onSuccess();

        @Override
        public void onFingerprintSuccess(Cipher cipher) {
            if (cipher != null) {
                this.encryptionManager.setCipher(cipher);
            } else {
                this.encryptionManager.recreateCipher();
            }
            if (mActivity.get() != null) {
                mActivity.get().runOnUiThread(this::onSuccess);
            }
        }

        @Override
        public void onFingerprintError(String error) {
            if (mActivity.get() != null) {
                mActivity.get().runOnUiThread(() ->
                        Toast.makeText(mActivity.get(), "Fingerprint Authentication failed " + error, Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public void onFingerprintCancel() {
            if (mActivity.get() != null) {
                mActivity.get().runOnUiThread(() ->
                        Toast.makeText(mActivity.get(), "Fingerprint Authentication failed", Toast.LENGTH_SHORT).show());
            }
        }
    }
}
