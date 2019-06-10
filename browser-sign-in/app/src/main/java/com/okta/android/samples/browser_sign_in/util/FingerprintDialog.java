/*
 * Copyright (C) 2016 Frederik Schweiger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.okta.android.samples.browser_sign_in.util;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.okta.android.samples.browser_sign_in.R;
import com.okta.oidc.storage.security.EncryptionManager;

import java.lang.ref.WeakReference;

@TargetApi(23)
public class FingerprintDialog extends DialogFragment {
    public static final int ANIMATION_DURATION = 500;

    protected TextView mTextViewStatus;
    protected ImageView mImageViewStatus;

    private FingerprintManager mFingerprintManager;
    private CancellationSignal mCancellationSignal;
    private FingerprintManager.CryptoObject mCryptoObject;
    private Context mContext;
    private int mPurpose;
    private WeakReference<FingerprintDialogCallbacks> mFingerprintDialogCallbacks;

    public static FingerprintDialog newInstance() {
        return new FingerprintDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);

        mContext = getActivity().getApplicationContext();
        mFingerprintManager = (FingerprintManager)
                mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        this.setCancelable(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
        }

        try {
            // Start listening for fingerprint events
            mFingerprintManager.authenticate(mCryptoObject, mCancellationSignal,
                    0, new AuthCallbacks(), null);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            // Should never be thrown since we have declared the USE_FINGERPRINT permission
            // in the manifest
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        cancel();
    }

    private void cancel() {
        if(mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.fragment_fingerprint, container);
        mTextViewStatus = content.findViewById(R.id.textViewFingerprintStatus);
        mImageViewStatus = content.findViewById(R.id.imageViewFingerprintStatus);
        content.findViewById(R.id.buttonFingerprintCancel).setOnClickListener(v -> {
            if (mFingerprintDialogCallbacks != null && mFingerprintDialogCallbacks.get() != null) {
                mFingerprintDialogCallbacks.get().onFingerprintCancel();
            }
            dismiss();
        });

        getDialog().setTitle(R.string.fingerprint_title);

        return content;
    }

    /**
     * Should be called before the dialog is shown in order to provide a valid CryptoObject.
     *
     * @param purpose The purpose for which you want to use the CryptoObject
     * @param object  The CryptoObject we want to authenticate for
     */
    public void init(int purpose, FingerprintManager.CryptoObject object, FingerprintDialogCallbacks fingerprintDialogCallbacks) {
        mCryptoObject = object;
        mPurpose = purpose;
        mFingerprintDialogCallbacks = new WeakReference<>(fingerprintDialogCallbacks);
    }

    /**
     * Updates the status text in the dialog with the provided error message.
     *
     * @param text represents the error message which will be shown
     */
    private void showErrorText(CharSequence text) {
        mImageViewStatus.setImageResource(R.drawable.ic_fingerprint_error);
        mTextViewStatus.setText(text);
        mTextViewStatus.setTextColor(Color.RED);

        mImageViewStatus.animate()
                .rotationBy(90)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .setDuration(ANIMATION_DURATION);
    }

    /**
     * Updates the status text in the dialog with a success text.
     */
    private void showSuccessText() {
        mImageViewStatus.setImageResource(R.drawable.ic_fingerprint_done);
        mTextViewStatus.setText(getString(R.string.fingerprint_success));
        mTextViewStatus.setTextColor(Color.GREEN);


        mImageViewStatus.setRotation(60);
        mImageViewStatus.animate()
                .rotation(0)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .setDuration(ANIMATION_DURATION)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // Empty
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FragmentTransaction transaction = getFragmentManager().beginTransaction()
                                .remove(FingerprintDialog.this);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            transaction.commitNow();
                        } else {
                            transaction.commit();
                        }
                        if (mFingerprintDialogCallbacks != null && mFingerprintDialogCallbacks.get() != null) {
                            mFingerprintDialogCallbacks.get().onFingerprintSuccess(mPurpose, mCryptoObject);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        // Empty
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        // Empty
                    }
                });
    }

    /**
     * The interface which the calling activity needs to implement.
     */
    public interface FingerprintDialogCallbacks {
        void onFingerprintSuccess(int purpose, FingerprintManager.CryptoObject cryptoObject);

        void onFingerprintCancel();
    }

    /**
     * This class represents the callbacks invoked by the FingerprintManager class.
     */
    class AuthCallbacks extends FingerprintManager.AuthenticationCallback {

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            showErrorText(getString(R.string.fingerprint_failed));
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            showErrorText(errString);
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
            showErrorText(helpString);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            showSuccessText();
        }
    }

    public static abstract class FingerPrintCallback implements FingerprintDialog.FingerprintDialogCallbacks {
        private EncryptionManager encryptionManager;
        private WeakReference<Context> mContext;

        public FingerPrintCallback(Context context, EncryptionManager encryptionManager) {
            this.mContext = new WeakReference<>(context);
            this.encryptionManager = encryptionManager;
        }

        protected abstract void onSuccess();

        @Override
        public void onFingerprintSuccess(int purpose, FingerprintManager.CryptoObject cryptoObject) {
            this.encryptionManager.recreateCipher();
            onSuccess();
        }

        @Override
        public void onFingerprintCancel() {
            if (mContext.get() != null) {
                Toast.makeText(mContext.get(), "Fingerprint Authentication failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
