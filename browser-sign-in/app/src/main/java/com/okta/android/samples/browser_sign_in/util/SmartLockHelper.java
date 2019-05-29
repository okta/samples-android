package com.okta.android.samples.browser_sign_in.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;

import com.okta.android.samples.browser_sign_in.R;

import javax.crypto.Cipher;

import static android.app.Activity.RESULT_OK;

public class SmartLockHelper {
    private static final int REQUEST_CODE_CREDENTIALS = 100;
    private static final String FINGERPRINT_DIALOG_TAG = "FINGERPRINT_DIALOG_TAG";

    private FingerprintDialog mFingerprintDialog;
    private FingerprintDialog.FingerprintDialogCallbacks credentialsCallback;

    public SmartLockHelper() {
        mFingerprintDialog = FingerprintDialog.newInstance();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREDENTIALS) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                if(credentialsCallback != null) {
                    credentialsCallback.onFingerprintSuccess(0, null);
                }
            } else {
                credentialsCallback.onFingerprintCancel();
            }
            credentialsCallback = null;
        }
    }

    public void showSmartLockChooseDialog(Activity activity, FingerprintDialog.FingerprintDialogCallbacks callback) {
        String[] types = new String[]{activity.getString(R.string.fingerprint_type), activity.getString(R.string.unlock_screen_type)};
        AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.select_smartlock_type_message)
                .setSingleChoiceItems(types, -1, (dialog, which) -> {
                    if(activity.getString(R.string.fingerprint_type).equalsIgnoreCase(types[which])) {
                        showFingerprint(activity, callback);
                        dialog.dismiss();
                    } else if(activity.getString(R.string.unlock_screen_type).equalsIgnoreCase(types[which])) {
                        credentialsCallback = callback;
                        showConfirmCredentials(activity);
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).create();

        alertDialog.show();
    }

    @TargetApi(21)
    private void showConfirmCredentials(Activity activity) {
        KeyguardManager keyguardManager = (KeyguardManager)activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.unlock_screen_title), "");
        if (intent != null) {
            activity.startActivityForResult(intent, REQUEST_CODE_CREDENTIALS);
        }
    }

    private void showFingerprint(Activity activity, FingerprintDialog.FingerprintDialogCallbacks callback) {
        if (mFingerprintDialog != null && activity.getFragmentManager().findFragmentByTag(FINGERPRINT_DIALOG_TAG) == null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                mFingerprintDialog.init(Cipher.DECRYPT_MODE, new FingerprintManager.CryptoObject(cipher), callback);
//            } else {
                mFingerprintDialog.init(Cipher.DECRYPT_MODE, null, callback);
//            }
            mFingerprintDialog.show(activity.getFragmentManager(), FINGERPRINT_DIALOG_TAG);
        }
    }
}
