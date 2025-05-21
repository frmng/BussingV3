package com.kmd.bussing.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.kmd.bussing.R;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<ItemLists> itemListsArrayList;

    public ItemAdapter(Context context, ArrayList<ItemLists> itemListsArrayList) {
        this.context = context;
        this.itemListsArrayList = itemListsArrayList;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView itemText;
        public ImageView itemIllustrations;

        public CardViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardView);
            itemText = view.findViewById(R.id.titleText);
            itemIllustrations = view.findViewById(R.id.illustration);

        }
    }

    @Override
    public ItemAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflater
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new ItemAdapter.CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        ItemLists itemLists = itemListsArrayList.get(position);

        holder.itemText.setText(itemLists.getItemText());
        holder.itemIllustrations.setImageResource(itemLists.getIllustration());
        try {
            int color = Color.parseColor(itemLists.getBackgroundColor());
            holder.cardView.setCardBackgroundColor(color);
        } catch (IllegalArgumentException e) {
            holder.cardView.setCardBackgroundColor(Color.LTGRAY); // fallback color
        }
    }

    @Override
    public int getItemCount() {
        return itemListsArrayList.size();
    }

}
