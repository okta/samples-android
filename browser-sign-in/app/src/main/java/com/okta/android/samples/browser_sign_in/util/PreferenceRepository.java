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

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceRepository {
    private final String FILENAME = "PREFERENCES_FILE";
    private final String SMARTLOCK_KEY = "SMARTLOCK_KEY";
    SharedPreferences sharedPreferences;

    public PreferenceRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
    }

    public void enableSmartLock(boolean enable) {
        this.sharedPreferences.edit().putBoolean(SMARTLOCK_KEY, enable).apply();
    }

    public boolean isEnabledSmartLock() {
        return this.sharedPreferences.getBoolean(SMARTLOCK_KEY, false);
    }
}
