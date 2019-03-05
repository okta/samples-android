package com.okta.android.samples.custom_sign_in.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.okta.android.samples.custom_sign_in.R;

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
            Context context = mContext.get();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(false); // if you want user to wait for some process to finish,
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = mInflater.inflate(R.layout.layout_progress_dialog, null, false);
            String textViewMessage = (message == null) ? mContext.get().getString(R.string.progress_dialog_message) : message;
            ((TextView)view.findViewById(R.id.message_textview)).setText(textViewMessage);
            builder.setView(R.layout.layout_progress_dialog);
            return builder.create();
        }
        return null;
    }
}