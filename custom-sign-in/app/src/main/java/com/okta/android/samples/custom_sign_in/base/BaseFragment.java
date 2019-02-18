package com.okta.android.samples.custom_sign_in.base;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.okta.android.samples.custom_sign_in.util.INavigation;
import com.okta.authn.sdk.client.AuthenticationClient;

public class BaseFragment extends Fragment {

    protected ILoadingView loadingView;
    protected IMessageView messageView;
    protected AuthenticationClient authenticationClient;
    protected INavigation navigation;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ILoadingView
                && context instanceof IMessageView
                && context instanceof IOktaAuthenticationClientProvider){
            loadingView = (ILoadingView)context;
            messageView = (IMessageView) context;
            authenticationClient = ((IOktaAuthenticationClientProvider)context).provideAuthenticationClient();
            navigation = ((INavigationProvider)context).provideNavigation();
        }
    }
}
