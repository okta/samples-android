package com.okta.totp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.okta.totp.model.Token;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.ViewHolder> {
    private List<Token> mTokens = new ArrayList<>();
    private WeakReference<ItemRemoveListener> removeListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final int TOKEN_REFRESH_PERIOD = 1000;
        private ScheduledThreadPoolExecutor updateThreadExecutor = new ScheduledThreadPoolExecutor(1);
        private ScheduledFuture scheduledFuture;
        TextView issuerTextView;
        TextView nameTextView;
        TextView codeTextView;
        ImageView removeBtn;

        ViewHolder(View v) {
            super(v);
            issuerTextView = v.findViewById(R.id.issuer_textview);
            codeTextView = v.findViewById(R.id.code_textview);
            nameTextView = v.findViewById(R.id.name_textView);
            removeBtn = v.findViewById(R.id.remove_btn);

        }

        public void runUpdateTimer(final Token token) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            scheduledFuture = updateThreadExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    codeTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            codeTextView.setText(token.getCurrentPassword());
                        }
                    });
                }
            }, 0L, TOKEN_REFRESH_PERIOD, TimeUnit.MILLISECONDS);
        }
    }

    public TokenAdapter(List<Token> tokens, ItemRemoveListener itemRemoveListener) {
        this.mTokens.addAll(tokens);
        this.removeListener = new WeakReference<>(itemRemoveListener);
    }

    @Override
    public TokenAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.token_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Token token = mTokens.get(position);
        holder.issuerTextView.setText(token.getIssuer());
        holder.nameTextView.setText(token.getName());
        holder.codeTextView.setText(token.getCurrentPassword());
        holder.removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (removeListener != null && removeListener.get() != null) {
                    removeListener.get().onRemove(token);
                }
            }
        });
        holder.runUpdateTimer(token);

    }

    public int add(Token token) {
        this.mTokens.add(token);
        return this.mTokens.size() - 1;
    }

    public void updateTokens(List<Token> tokens) {
        this.mTokens.clear();
        this.mTokens.addAll(tokens);
    }

    public void remove(Token token) {
        this.mTokens.remove(token);
    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    public interface ItemRemoveListener {
        void onRemove(Token token);
    }
}
