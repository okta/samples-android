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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;

import com.okta.android.samples.browser_sign_in.R;

import java.lang.ref.WeakReference;


@TargetApi(23)
@Deprecated
public class FingerprintDialog extends DialogFragment {
    public static final int ANIMATION_DURATION = 500;

    protected TextView mTextViewStatus;
    protected ImageView mImageViewStatus;

    private FingerprintManagerCompat mFingerprintManagerCompat;
    private CancellationSignal mCancellationSignal;
    private FingerprintManagerCompat.CryptoObject mCryptoObject;
    private Context mContext;
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
        mFingerprintManagerCompat = FingerprintManagerCompat.from(mContext);
        this.setCancelable(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
        }

        try {
            FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(mCryptoObject != null ? mCryptoObject.getCipher() : null);
            mFingerprintManagerCompat.authenticate(cryptoObject, 0, mCancellationSignal, new FingerprintManagerCompat.AuthenticationCallback() {
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
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    showSuccessText();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    showErrorText(getString(R.string.fingerprint_failed));
                }
            }, null);
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
        if (mCancellationSignal != null) {
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

    public void init(FingerprintManagerCompat.CryptoObject object, FingerprintDialogCallbacks fingerprintDialogCallbacks) {
        mCryptoObject = object;
        mFingerprintDialogCallbacks = new WeakReference<>(fingerprintDialogCallbacks);
    }

    private void showErrorText(CharSequence text) {
        mImageViewStatus.setImageResource(R.drawable.ic_fingerprint_error);
        mTextViewStatus.setText(text);
        mTextViewStatus.setTextColor(Color.RED);

        mImageViewStatus.animate()
                .rotationBy(90)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .setDuration(ANIMATION_DURATION);
    }

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
                            mFingerprintDialogCallbacks.get().onFingerprintSuccess();
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

    public interface FingerprintDialogCallbacks {
        void onFingerprintSuccess();

        void onFingerprintError(String error);

        void onFingerprintCancel();
    }
}
