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
package com.okta.android.samples.custom_sign_in.util;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class NavigationHelper implements INavigation {
    private Activity activity;
    private FragmentManager manager;
    private int container;

    public NavigationHelper(Activity activity, FragmentManager fragmentManager, int container) {
        this.activity = activity;
        this.manager = fragmentManager;
        this.container = container;
    }

    @Override
    public void close() {
        this.activity.finish();
    }

    @Override
    public void present(Fragment fragment) {
        this.manager.beginTransaction()
                .replace(container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void push(Fragment fragment) {
        this.manager.beginTransaction()
                .add(container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
