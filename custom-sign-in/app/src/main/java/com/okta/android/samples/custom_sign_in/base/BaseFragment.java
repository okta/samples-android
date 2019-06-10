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
package com.okta.android.samples.custom_sign_in.base;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.Fragment;

import com.okta.android.samples.custom_sign_in.util.INavigation;
import com.okta.authn.sdk.client.AuthenticationClient;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaseFragment extends Fragment {
    private static String TAG = "BaseFragment";
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected Future lastTask = null;

    protected AuthenticationClient authenticationClient;
    protected INavigation navigation;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ILoadingView
                && context instanceof IMessageView
                && context instanceof IOktaAuthenticationClientProvider){
            authenticationClient = ((IOktaAuthenticationClientProvider)context).provideAuthenticationClient();
            navigation = ((INavigationProvider)context).provideNavigation();
        }
    }

    protected void showLoading() {
        Activity activity = getActivity();
        if((activity instanceof ILoadingView)) {
            ((ILoadingView) activity).show();
        }
    }

    protected void hideLoading() {
        Activity activity = getActivity();
        if((activity instanceof ILoadingView)) {
            ((ILoadingView) activity).hide();
        }
    }

    protected void showMessage(String message) {
        Activity activity = getActivity();
        if((activity instanceof IMessageView)) {
            ((IMessageView) activity).showMessage(message);
        }
    }

    protected void submit(Runnable task) {
        if(!executor.isShutdown()) {
            lastTask = executor.submit(task);
        }
    }
    protected void schedule(Runnable task, long delay, TimeUnit unit) {
        if(!executor.isShutdown()) {
            lastTask = executor.schedule(task, delay, unit);
        }
    }

    private void unsubscribe() {
        executor.shutdownNow();
    }

    protected void runOnUIThread(Runnable task) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(task);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }
}
