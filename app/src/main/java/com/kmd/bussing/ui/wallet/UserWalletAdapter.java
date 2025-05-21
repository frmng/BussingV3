package com.kmd.bussing.ui.wallet;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import com.kmd.bussing.R;

public class UserWalletAdapter extends RecyclerView.Adapter<UserWalletAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<UserWalletList> userWalletListArrayList;
    private boolean expanded = false;

    public UserWalletAdapter(Context context) {
        this.context = context;
        this.userWalletListArrayList = new ArrayList<>();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public TextView userActivity, timeStamp, userId, transactionPrice;

        public CardViewHolder(View view) {
            super(view);
            userActivity = view.findViewById(R.id.userActivity);
            timeStamp = view.findViewById(R.id.timeStamp);
            userId = view.findViewById(R.id.userId);
            transactionPrice = view.findViewById(R.id.transactionPrice);
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_wallet_transaction, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        UserWalletList transaction = userWalletListArrayList.get(position);

        holder.userActivity.setText(transaction.getUserActivity() != null ? transaction.getUserActivity() : "--");
        holder.timeStamp.setText(transaction.getTransactionTimeStamp() != null ? transaction.getTransactionTimeStamp() : "--");
        holder.userId.setText(transaction.getUserId() != null ? transaction.getUserId() : "--");


        // Set transaction price format
        double price = transaction.getTransactionPrice();
        if (price < 0) {
            holder.transactionPrice.setText("-₱" + Math.abs(price));
            holder.transactionPrice.setTextColor(Color.parseColor("#ff0000"));
        } else {
            holder.transactionPrice.setText("+₱" + price);
            holder.transactionPrice.setTextColor(Color.parseColor("#03c03c"));
        }

        // Determine last visible item index
        int lastVisibleItem = expanded ? userWalletListArrayList.size() - 1 : 4;

        // Apply different background styles
        if (position == 0) {
            holder.itemView.findViewById(R.id.cardWalletTransaction).setBackgroundResource(R.drawable.rounded_top);
        } else if (position == lastVisibleItem) {
            holder.itemView.findViewById(R.id.cardWalletTransaction).setBackgroundResource(R.drawable.rounded_bottom);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            layoutParams.bottomMargin = 20;
            holder.itemView.setLayoutParams(layoutParams);
        } else {
            holder.itemView.findViewById(R.id.cardWalletTransaction).setBackgroundResource(R.drawable.flat_card);

            // Reset margin for non-last items
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            layoutParams.bottomMargin = 0;
            holder.itemView.setLayoutParams(layoutParams);
        }

    }

    public void setExpanded(boolean isExpanded) {
        this.expanded = isExpanded;
        if (toggleListener != null) {
            toggleListener.onToggle(expanded);
        }
        notifyDataSetChanged();
    }


    public interface OnExpandToggleListener {
        void onToggle(boolean expanded);
    }

    private OnExpandToggleListener toggleListener;

    public void setOnExpandToggleListener(OnExpandToggleListener listener) {
        this.toggleListener = listener;
    }


    @Override
    public int getItemCount() {
        // Show only the latest 5 transactions if not expanded
        if (!expanded && userWalletListArrayList.size() > 5) {
            return 5;
        }
        return userWalletListArrayList.size();
    }

    public void updateTransactions(ArrayList<UserWalletList> newTransactions) {
        this.userWalletListArrayList.clear();
        this.userWalletListArrayList.addAll(newTransactions);

        Collections.sort(this.userWalletListArrayList, new Comparator<UserWalletList>() {
            @Override
            public int compare(UserWalletList t1, UserWalletList t2) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm ddMMMyy", Locale.ENGLISH);
                try {
                    Date date1 = sdf.parse(t1.getTransactionTimeStamp());
                    Date date2 = sdf.parse(t2.getTransactionTimeStamp());
                    if (date1 != null && date2 != null) {
                        return date2.compareTo(date1);
                    }
                } catch (ParseException e) {
                    Log.e("DateParseError", "Failed to parse timestamp: " + e.getMessage());
                }
                return 0;
            }
        });

        notifyDataSetChanged();
    }

}
