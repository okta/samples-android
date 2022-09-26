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
package com.okta.totp.model;

import com.marcelkliemannel.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

public class Token implements Serializable {
    private PersistableToken persistableToken;
    private TimeBasedOneTimePasswordGenerator timeBasedOneTimePasswordGenerator;

    public Token(PersistableToken persistableToken, TimeBasedOneTimePasswordGenerator timeBasedOneTimePasswordGenerator) {
        this.persistableToken = persistableToken;
        this.timeBasedOneTimePasswordGenerator = timeBasedOneTimePasswordGenerator;
    }

    public String getName() {
        return persistableToken.getName();
    }

    public String getIssuer() {
        return persistableToken.getIssuer();
    }

    public String getCurrentPassword() {
        return timeBasedOneTimePasswordGenerator.generate(new Date(System.currentTimeMillis()));
    }

    public JSONObject toJSON() throws JSONException {
        return persistableToken.toJSON();
    }
}
