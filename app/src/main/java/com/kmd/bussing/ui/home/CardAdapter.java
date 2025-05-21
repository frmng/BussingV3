package com.kmd.bussing.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
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
import android.view.ViewGroup;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.ticket.TicketFragment;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<CardLists> cardListsArrayList;

    public CardAdapter(Context context, ArrayList<CardLists> cardListsArrayList) {
        this.context = context;
        this.cardListsArrayList = cardListsArrayList;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView busNum, fromLoc, toWhereLoc, currentLoc, departureTime, driver, conductor, capacity, price, status;
        public ImageView busCompImage;
        public Button bookButton;

        public CardViewHolder(View view) {
            super(view);
            // Initialize the views
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

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflater
        View view = LayoutInflater.from(context).inflate(R.layout.card_layout, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        CardLists cardLists = cardListsArrayList.get(position);

        // Set the data for the current card
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

        //pricing logic
        int basePrice = 105;
        int regularPrice = 120;
        int price;

        if("Bulakan".equalsIgnoreCase(cardLists.getFromLocation()) && "Cubao".equalsIgnoreCase(cardLists.getToWhereLocation())) {
            price = basePrice;

        } else {
            price = regularPrice;
        }

        holder.price.setText("₱" + price+".00");

        LinearLayout detailsLayout = holder.itemView.findViewById(R.id.details);
        ImageView dropdownImage = holder.itemView.findViewById(R.id.dropdown);

        // Initially hide details
        detailsLayout.setVisibility(View.GONE);
        detailsLayout.getLayoutParams().height = 0;

        // Toggle dropdown
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
