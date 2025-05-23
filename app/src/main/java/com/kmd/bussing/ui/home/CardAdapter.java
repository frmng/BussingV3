package com.kmd.bussing.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.ticket.TicketFragment;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<CardLists> cardListsArrayList;
    private FirebaseFirestore firestore;

    public CardAdapter(Context context, ArrayList<CardLists> cardListsArrayList) {
        this.context = context;
        this.cardListsArrayList = cardListsArrayList;
        firestore = FirebaseFirestore.getInstance();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView busNum, fromLoc, toWhereLoc, currentLoc, departureTime, driver, conductor, capacity, price, status;
        public ImageView busCompImage;
        public Button bookButton;

        public CardViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardView);
            busNum = view.findViewById(R.id.busNumber);
            fromLoc = view.findViewById(R.id.fromLocation);
            toWhereLoc = view.findViewById(R.id.toWhereLocation);
            currentLoc = view.findViewById(R.id.currentLocation);
            departureTime = view.findViewById(R.id.departureTime);
            busCompImage = view.findViewById(R.id.busCompany);
            driver = view.findViewById(R.id.driverName);
            conductor = view.findViewById(R.id.conductorName);
            capacity = view.findViewById(R.id.capacity);
            price = view.findViewById(R.id.price);
            status = view.findViewById(R.id.status);
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_layout, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardLists cardLists = cardListsArrayList.get(position);

        holder.busNum.setText(cardLists.getBusNumber());
        holder.fromLoc.setText(cardLists.getFromLocation());
        holder.toWhereLoc.setText(cardLists.getToWhereLocation());
        holder.currentLoc.setText(cardLists.getCurrentLocation());
        holder.departureTime.setText(cardLists.getDepartureTime());
        holder.busCompImage.setImageResource(cardLists.getBusCompanyImage());
        holder.driver.setText(cardLists.getDriverName());
        holder.conductor.setText(cardLists.getConductorName());
        holder.capacity.setText(String.valueOf(cardLists.getCapacity()));
        holder.status.setText(cardLists.getStatus());

        // Fetch dynamic price from Firestore
        firestore.collection("ScheduleDocumentsCollection")
                .whereEqualTo("from", cardLists.getFromLocation())
                .whereEqualTo("to", cardLists.getToWhereLocation())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Log.d("CardError", "Document found: " + doc.getId());

                            if (doc.contains("price")) {
                                Object priceObj = doc.get("price");

                                if (priceObj != null) {
                                    try {
                                        int fetchedPrice = Integer.parseInt(priceObj.toString());
                                        Log.d("CardError", "Fetched price: " + fetchedPrice);
                                        holder.price.setText("₱" + fetchedPrice + ".00");
                                    } catch (NumberFormatException e) {
                                        Log.e("CardError", "Invalid number format in price field: " + priceObj, e);
                                        holder.price.setText("₱ N/A");
                                    }
                                } else {
                                    Log.w("CardError", "Price field is null");
                                    holder.price.setText("₱ N/A");
                                }

                            } else {
                                Log.w("CardError", "Document does not contain 'price' field");
                                holder.price.setText("₱ N/A");
                            }

                            break; // Stop after the first match
                        }
                    } else {
                        Log.w("CardError", "No matching documents found for price");
                        holder.price.setText("₱ N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CardError", "Failed to fetch price", e);
                    holder.price.setText("₱ N/A");
                });

        LinearLayout detailsLayout = holder.itemView.findViewById(R.id.details);
        ImageView dropdownImage = holder.itemView.findViewById(R.id.dropdown);

        detailsLayout.setVisibility(View.GONE);
        detailsLayout.getLayoutParams().height = 0;

        holder.cardView.setOnClickListener(v -> {
            if (detailsLayout.getVisibility() == View.GONE) {
                detailsLayout.setVisibility(View.VISIBLE);
                detailsLayout.measure(View.MeasureSpec.makeMeasureSpec(holder.itemView.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                int targetHeight = detailsLayout.getMeasuredHeight();

                ValueAnimator expandAnimator = ValueAnimator.ofInt(0, targetHeight);
                expandAnimator.addUpdateListener(valueAnimator -> {
                    detailsLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                    detailsLayout.requestLayout();
                });
                expandAnimator.setDuration(300);
                expandAnimator.start();

                ObjectAnimator.ofFloat(dropdownImage, "rotation", 0f, 180f).setDuration(300).start();
            } else {
                ValueAnimator collapseAnimator = ValueAnimator.ofInt(detailsLayout.getHeight(), 0);
                collapseAnimator.addUpdateListener(valueAnimator -> {
                    detailsLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                    detailsLayout.requestLayout();
                });
                collapseAnimator.setDuration(300);
                collapseAnimator.start();

                collapseAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        detailsLayout.setVisibility(View.GONE);
                        detailsLayout.getLayoutParams().height = 0;
                        detailsLayout.requestLayout();
                    }
                });

                ObjectAnimator.ofFloat(dropdownImage, "rotation", 180f, 0f).setDuration(300).start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardListsArrayList.size();
    }
}
