package com.kmd.bussing.ui.status;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.kmd.bussing.R;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<StatusLists> statusListsArrayList;

    public StatusAdapter(Context context) {
        this.context = context;
        this.statusListsArrayList = new ArrayList<>();
        fetchBusSchedules();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView busNum, dpTime, busFrom, busTo, price, busStatus;

        public CardViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardBusStatus);
            busNum = view.findViewById(R.id.busNo);
            dpTime = view.findViewById(R.id.dpTime);
            busFrom = view.findViewById(R.id.from);
            busTo = view.findViewById(R.id.toWhere);
            price = view.findViewById(R.id.price);
            busStatus = view.findViewById(R.id.busStatus);
        }
    }

    @NonNull
    @Override
    public StatusAdapter.CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_bus_status, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        try {
            StatusLists status = statusListsArrayList.get(position);

            // Defensive null checks
            String busNo = status.getBusNo() != null ? status.getBusNo() : "0";
            String dpTime = status.getBusDP() != null ? status.getBusDP() : "N/A";
            String busFrom = status.getBusFrom() != null ? status.getBusFrom() : "N/A";
            String busTo = status.getBusTo() != null ? status.getBusTo() : "N/A";
            String price = status.getBusPrice() != null ? status.getBusPrice() : "0.00";
            String busStatus = status.getStatus() != null ? status.getStatus() : "Unknown";

            holder.busNum.setText("BUS" + busNo);
            holder.dpTime.setText("DP: " + dpTime);
            holder.busFrom.setText(busFrom);
            holder.busTo.setText(busTo);
            holder.price.setText("₱" + price);
            holder.busStatus.setText(busStatus);

            if ("Active".equalsIgnoreCase(busStatus)) {
                holder.busStatus.setTextColor(Color.parseColor("#03c03c"));
            } else if ("Inactive".equalsIgnoreCase(busStatus)) {
                holder.busStatus.setTextColor(Color.parseColor("#FF0000"));
            } else {
                holder.busStatus.setTextColor(Color.GRAY);
            }

        } catch (Exception e) {
            Log.e("StatusAdapter", "Error in onBindViewHolder", e);
        }
    }

    @Override
    public int getItemCount() {
        return statusListsArrayList.size();
    }

    public void fetchBusSchedules() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ScheduleDocumentsCollection")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error fetching data", error);
                        return;
                    }

                    if (value != null) {
                        statusListsArrayList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            String busNo = document.getString("busNo");
                            String dpTime = document.getString("departureTime");
                            String busFrom = document.getString("from");
                            String busTo = document.getString("to");

                            // ✅ Safely get and parse price
                            String priceString = document.getString("price");
                            String price;
                            try {
                                double priceDouble = Double.parseDouble(priceString != null ? priceString : "0");
                                price = String.format("%.2f", priceDouble);
                            } catch (NumberFormatException e) {
                                price = "0.00";
                                Log.e("StatusAdapter", "Invalid price format: " + priceString, e);
                            }

                            String status = document.getString("status");

                            statusListsArrayList.add(new StatusLists(busNo, busFrom, busTo, dpTime, price, status));
                        }

                        // Sort by bus number in descending order
                        Collections.sort(statusListsArrayList, new Comparator<StatusLists>() {
                            @Override
                            public int compare(StatusLists o1, StatusLists o2) {
                                try {
                                    int busNum1 = Integer.parseInt(o1.getBusNo());
                                    int busNum2 = Integer.parseInt(o2.getBusNo());
                                    return Integer.compare(busNum2, busNum1);
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            }
                        });

                        notifyDataSetChanged();
                    }
                });
    }

}
