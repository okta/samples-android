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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.okta.android.samples.browser_sign_in.R;

import java.lang.ref.WeakReference;

public class OktaProgressDialog {
    WeakReference<Context> mContext;
    AlertDialog dialog;

    public OktaProgressDialog(Context context) {
        this.mContext = new WeakReference<>(context);
    }

    public void show() {
        show(null);
    }

    public void show(String message) {
        if(this.dialog == null) {
            this.dialog = createAlertDialog(message);
        }
        if(this.dialog != null) {
            dialog.show();
        }
    }

    public void hide() {
        if(this.dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    private AlertDialog createAlertDialog(String message) {
        if(mContext.get() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext.get());
            builder.setCancelable(false); // if you want user to wait for some process to finish,
            LayoutInflater mInflater = (LayoutInflater) mContext.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = mInflater.inflate(R.layout.layout_progress_dialog, null, false);
            String textViewMessage = (message == null) ? mContext.get().getString(R.string.progress_dialog_message) : message;
            ((TextView)view.findViewById(R.id.message_textview)).setText(textViewMessage);
            builder.setView(view);
            return builder.create();
        }
        return null;
    }
}