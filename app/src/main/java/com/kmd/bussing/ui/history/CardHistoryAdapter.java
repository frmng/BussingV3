package com.kmd.bussing.ui.history;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.history.CardHistoryAdapter;
import com.kmd.bussing.ui.history.CardHistoryLists;
import com.kmd.bussing.ui.status.StatusAdapter;
import com.kmd.bussing.ui.status.StatusLists;

public class CardHistoryAdapter extends RecyclerView.Adapter<CardHistoryAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<CardHistoryLists> cardHistoryListsArrayList;
    private FirebaseFirestore db;
    private String currentUserId;
    private boolean isSelectionMode = false;
    private ArrayList<CardHistoryLists> selectedItems = new ArrayList<>();

    public CardHistoryAdapter(Context context, ArrayList<CardHistoryLists> cardHistoryLists) {
        this.context = context;
        this.cardHistoryListsArrayList = cardHistoryLists;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }


    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView historyFrom, historyTo, historyTicketNo, historyTicketPrice, historyTimeStamp;
        public CheckBox checkBox;


        public CardViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardHistory);
            historyFrom = view.findViewById(R.id.from);
            historyTo = view.findViewById(R.id.toWhere);
            historyTicketNo = view.findViewById(R.id.ticketNo);
            historyTicketPrice = view.findViewById(R.id.ticketPrice);
            historyTimeStamp = view.findViewById(R.id.timeStamp);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }

    @NonNull
    @Override
    public CardHistoryAdapter.CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_history, parent, false);
        return new CardHistoryAdapter.CardViewHolder(view);


    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardHistoryLists history = cardHistoryListsArrayList.get(position);
        holder.historyFrom.setText(history.getTransactionFrom());
        holder.historyTo.setText(history.getTransactionTo());
        holder.historyTicketNo.setText(history.getTransactionTicketNo());
        holder.historyTicketPrice.setText("₱" + history.getTransactionPrice());
        holder.historyTimeStamp.setText(history.getTransactionTimeStamp());

        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(history));

        // Long press to enter selection mode and select the first item
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                selectedItems.clear();
                selectedItems.add(history);
                notifyDataSetChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(history)) {
                    selectedItems.remove(history);
                    holder.checkBox.setChecked(false);
                } else {
                    selectedItems.add(history);
                    holder.checkBox.setChecked(true);
                }

                // Exit selection mode if no items are selected
                if (selectedItems.isEmpty()) {
                    exitSelectionMode();
                } else {
                    notifyDataSetChanged();
                }
            }
        });

        // Handle checkbox selection separately
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    selectedItems.add(history);
                } else {
                    selectedItems.remove(history);
                    if (selectedItems.isEmpty()) {
                        exitSelectionMode();
                    }
                }
            }
        });

        // Change card background color when pressed
        holder.cardView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holder.cardView.setCardBackgroundColor(Color.WHITE);
                    break;
            }
            return false;
        });
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return cardHistoryListsArrayList.size();
    }

    public void deleteSelectedItems() {
        if (selectedItems.isEmpty()) return;

        new AlertDialog.Builder(context)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ArrayList<CardHistoryLists> itemsToDelete = new ArrayList<>(selectedItems);
                    selectedItems.clear();

                    for (CardHistoryLists item : itemsToDelete) {
                        String ticketCode = item.getTransactionTicketNo(); // Ticket code may not be the document ID

                        db.collection("TicketGeneratedCollection")
                                .whereEqualTo("ticketCode", ticketCode) // Find document by ticketCode
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        String docId = document.getId(); // Get actual Firestore document ID
                                        db.collection("TicketGeneratedCollection").document(docId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("FirestoreDelete", "Successfully deleted from Firestore");
                                                    cardHistoryListsArrayList.remove(item);
                                                    notifyDataSetChanged();
                                                })
                                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting", e));
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error fetching document", e));
                    }


                    isSelectionMode = false;
                    notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public void fetchTicketHistory(Runnable callback) {
        CollectionReference ticketsRef = db.collection("TicketGeneratedCollection");

        ticketsRef.whereEqualTo("uid", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        return; // Handle error
                    }

                    cardHistoryListsArrayList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String formattedDate = "";

                            if (document.contains("createdAt")) {
                                Object createdAtObj = document.get("createdAt");
                                if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                    formattedDate = sdf.format(((com.google.firebase.Timestamp) createdAtObj).toDate());
                                } else if (createdAtObj instanceof String) {
                                    formattedDate = (String) createdAtObj;
                                }
                            }

                            CardHistoryLists history = new CardHistoryLists(
                                    document.getString("from"),
                                    document.getString("to"),
                                    document.getString("ticketCode"),
                                    document.getString("price"),
                                    formattedDate
                            );
                            cardHistoryListsArrayList.add(history);
                        }
                    }
                    notifyDataSetChanged();

                    if (callback != null) {
                        callback.run();
                    }
                });
    }

    private static final SimpleDateFormat parseFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public void sortByDateDescending() {
        cardHistoryListsArrayList.sort((o1, o2) -> {
            try {
                return parseFormat.parse(o2.getTransactionTimeStamp())
                        .compareTo(parseFormat.parse(o1.getTransactionTimeStamp()));
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
        notifyDataSetChanged();
    }

    public void sortByDateAscending() {
        cardHistoryListsArrayList.sort((o1, o2) -> {
            try {
                return parseFormat.parse(o1.getTransactionTimeStamp())
                        .compareTo(parseFormat.parse(o2.getTransactionTimeStamp()));
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
        notifyDataSetChanged();
    }


}
