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

package com.okta.android.samples.browser_sign_in.rxClient;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.clients.web.SyncWebAuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

class RxWebAuthClientImpl implements RxWebAuthClient {
    private WeakReference<FragmentActivity> mActivity;
    private SyncWebAuthClient mSyncAuthClient;
    private PublishSubject<RxResult> submitResults;
    private RxSessionClient rxSessionClient;

    RxWebAuthClientImpl(OIDCConfig oidcConfig, Context context, OktaStorage oktaStorage, EncryptionManager encryptionManager, HttpConnectionFactory connectionFactory, boolean requireHardwareBackedKeyStore, boolean cacheMode, @ColorInt
            int customTabColor, @Nullable String[] supportedBrowsers) {
        mSyncAuthClient = new SyncWebAuthClientFactory(customTabColor, supportedBrowsers).createClient(oidcConfig, context, oktaStorage, encryptionManager,
                connectionFactory, requireHardwareBackedKeyStore, cacheMode);
        rxSessionClient = new RxSessionClientImpl(mSyncAuthClient.getSessionClient());
    }

    @Override
    public Single<Boolean> isInProgress() {
        return Single.create(emitter -> emitter.onSuccess(mSyncAuthClient.isInProgress()));
    }

    @Override
    public Completable logIn(@NonNull FragmentActivity activity, AuthenticationPayload payload) {
        return new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                try {
                    mSyncAuthClient.signIn(activity, payload);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    processLogInResult(Result.cancel());
                    observer.onError(e);
                }
            }
        };
    }

    @Override
    public Completable signOutOfOkta(@NonNull FragmentActivity activity) {
        return new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                try {
                    mSyncAuthClient.signOutOfOkta(activity);
                    observer.onComplete();
                } catch (InterruptedException e) {
                    processSignOutResult(Result.cancel());
                    observer.onError(e);
                }
            }
        };
    }

    @Override
    public Observable<RxResult> registerListener(FragmentActivity activity) {
        submitResults = PublishSubject.create();

        registerActivityLifeCycle(activity);
        mSyncAuthClient.registerCallbackIfInterrupt(activity, (result, type) -> {
            switch (type) {
                case SIGN_IN:
                    processLogInResult(result);
                    break;
                case SIGN_OUT:
                    processSignOutResult(result);
                    break;
                default:
                    break;
            }
        }, Executors.newSingleThreadExecutor());

        return submitResults;
    }

    @Override
    public RxSessionClient getSessionClient() {
        return rxSessionClient;
    }

    private void registerActivityLifeCycle(@NonNull final FragmentActivity activity) {
        mActivity = new WeakReference<>(activity);
        mActivity.get().getApplication()
                .registerActivityLifecycleCallbacks(new EmptyActivityLifeCycle() {
                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (mActivity != null && mActivity.get() == activity) {
                            stop();
                            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                        }
                    }
                });
    }

    private void stop() {
        submitResults.onComplete();
        if (mActivity.get() != null) {
            mSyncAuthClient.unregisterCallback(mActivity.get());
        }
    }

    private void processLogInResult(Result result) {
        if (submitResults != null) {
            if(result.isSuccess()) {
                submitResults.onNext(new RxResult(AuthorizationStatus.AUTHORIZED, result));
            } else {
                submitResults.onNext(new RxResult(null, result));
            }
        }
    }

    private void processSignOutResult(Result result) {
        if (submitResults != null) {
            if(result.isSuccess()) {
                submitResults.onNext(new RxResult(AuthorizationStatus.SIGNED_OUT, result));
            } else {
                submitResults.onNext(new RxResult(null, result));
            }
        }
    }

    @Override
    public Completable migrateTo(EncryptionManager manager) {
        return rxSessionClient.migrateTo(manager);
    }

    public class RxResult {
        private AuthorizationStatus status;
        private Result result;

        RxResult(AuthorizationStatus status, Result result) {
            this.status = status;
            this.result = result;
        }

        public AuthorizationStatus getStatus() {
            return status;
        }

        public Result getResult() {
            return result;
        }
    }
}
