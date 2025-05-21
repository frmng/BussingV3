package com.kmd.bussing.ui.home;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.account.AccountFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<CardLists> cardLists = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerView staticRecyclerView;
    private CardAdapter cardAdapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private ShapeableImageView appBarProfile;
    private TextView appBarUsername;
    private TextView appBarEmail;
    private TextView userNameTextView;
    private Button bookNow;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        staticRecyclerView = view.findViewById(R.id.recyclerViewHorizontal);
        staticRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        ArrayList<ItemLists> itemLists = new ArrayList<>();
        itemLists.add(new ItemLists("Book Your Ride in Seconds—Anytime, Anywhere.", R.drawable.bus, "#FFB13D"));
        itemLists.add(new ItemLists("Your Journey Starts with One Tap.", R.drawable.one_tap, "#2FDBFA"));
        itemLists.add(new ItemLists("Skip the Lines. Pay in Seconds.", R.drawable.easy_payment, "#2097FF"));
        itemLists.add(new ItemLists("Get exclusive discounts every trip.", R.drawable.bus2, "#9FE087"));

        ItemAdapter staticAdapter = new ItemAdapter(getContext(), itemLists);
        staticRecyclerView.setAdapter(staticAdapter);

        db = FirebaseFirestore.getInstance();
        cardAdapter = new CardAdapter(getContext(), cardLists);
        recyclerView.setAdapter(cardAdapter);

        TextView dateTextView = view.findViewById(R.id.dateTextView);
        String currentDate = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
        dateTextView.setText(currentDate);

        appBarProfile = view.findViewById(R.id.appBarProfile);
        appBarUsername = view.findViewById(R.id.appbarUsername);
        appBarEmail = view.findViewById(R.id.appbarEmail);
        userNameTextView = view.findViewById(R.id.userName);
        bookNow = view.findViewById(R.id.bookNow);
        swipeRefreshLayout = view.findViewById(R.id.swipe);

        swipeRefreshLayout.setOnRefreshListener(this);

        updateUserInfo();
        appBarProfile.setOnClickListener(this::showPopupMenu);

        fetchDataFromFireStore();

        bookNow.setOnClickListener(v -> {
            BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.nav_view);
            bottomNavigationView.setSelectedItemId(R.id.navigation_ticket);
        });

        return view;
    }

    private void removeCardById(String documentId) {
        for (int i = 0; i < cardLists.size(); i++) {
            CardLists card = cardLists.get(i);
            // Extract bus number from documentId logic if needed
            String expectedBusNum = "BUS" + documentId;

            if (card.getBusNumber().equals(expectedBusNum)) {
                cardLists.remove(i);
                break;
            }
        }
    }


    @Override
    public void onRefresh() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cardLists.clear();
                fetchDataFromFireStore();
                cardAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000);
    }

    private void fetchDataFromFireStore() {
        listenerRegistration = db.collection("ScheduleDocumentsCollection")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore Error", error.getMessage());
                        return;
                    }

                    if (value == null) return;

                    cardLists.clear();

                    for (DocumentChange doc : value.getDocumentChanges()) {
                        String id = doc.getDocument().getId();

                        switch (doc.getType()) {
                            case ADDED:
                            case MODIFIED:
                                removeCardById(id);

                                final String arrivalPoint = doc.getDocument().getString("to");
                                final Object busNoObj = doc.getDocument().get("busNo");
                                final String busNumber = (busNoObj != null) ? "BUS" + busNoObj.toString() : "BUS";

                                final String fromLocation = doc.getDocument().getString("from");
                                final String currentLocation = fromLocation;
                                final String departureTime = doc.getDocument().getString("departureTime");
                                final String status = doc.getDocument().getString("status");

                                // Wrap availableSeats in an array so we can use it in the lambda
                                final int[] availableSeatsHolder = {0};
                                Object availableSeatsObj = doc.getDocument().get("availableSeats");
                                if (availableSeatsObj instanceof Number) {
                                    availableSeatsHolder[0] = ((Number) availableSeatsObj).intValue();
                                } else if (availableSeatsObj instanceof String) {
                                    try {
                                        availableSeatsHolder[0] = Integer.parseInt((String) availableSeatsObj);
                                    } catch (NumberFormatException e) {
                                        Log.e("Firestore Error", "Invalid availableSeats: " + availableSeatsObj);
                                    }
                                }

                                // Fetch driver and conductor from HomeDocumentsCollection
                                db.collection("HomeDocumentsCollection")
                                        .whereEqualTo("busNo", busNoObj)
                                        .get()
                                        .addOnSuccessListener(homeSnapshots -> {
                                            String driver = "N/A";
                                            String conductor = "N/A";

                                            if (!homeSnapshots.isEmpty()) {
                                                DocumentSnapshot homeDoc = homeSnapshots.getDocuments().get(0);
                                                driver = homeDoc.getString("busDriver");
                                                conductor = homeDoc.getString("busConductor");
                                            }

                                            // Add to the list and update adapter
                                            cardLists.add(new CardLists(
                                                    busNumber,
                                                    fromLocation,
                                                    arrivalPoint,
                                                    R.drawable.bus_front,
                                                    currentLocation,
                                                    departureTime,
                                                    driver,
                                                    conductor,
                                                    availableSeatsHolder[0],
                                                    0,
                                                    status
                                            ));
                                            cardAdapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore Error", "Failed to fetch driver/conductor: " + e.getMessage());

                                            // Add with default driver/conductor if fetch fails
                                            cardLists.add(new CardLists(
                                                    busNumber,
                                                    fromLocation,
                                                    arrivalPoint,
                                                    R.drawable.bus_front,
                                                    currentLocation,
                                                    departureTime,
                                                    "N/A",
                                                    "N/A",
                                                    availableSeatsHolder[0],
                                                    0,
                                                    status
                                            ));
                                            cardAdapter.notifyDataSetChanged();
                                        });
                                break;

                            case REMOVED:
                                removeCardById(id);
                                break;
                        }
                    }
                });
    }

    public void updateUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String username = user.getDisplayName();
            String email = user.getEmail();

            // Reference to the Realtime Database to get base64 image
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
            String userID = user.getUid();

            databaseReference.child(userID).child("profileImageBase64").get()
                    .addOnSuccessListener(snapshot -> {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            loadBase64Image(base64, appBarProfile);
                        } else {
                            // Fallback if base64 image is not available
                            Glide.with(getContext())
                                    .load(R.drawable.default_user1)
                                    .circleCrop()
                                    .into(appBarProfile);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Glide.with(getContext())
                                .load(R.drawable.default_user1)
                                .circleCrop()
                                .into(appBarProfile);
                    });

            appBarUsername.setText(username != null ? username : "Username");
            appBarEmail.setText(email != null ? email : "username@gmail.com");

            if (username != null) {
                String firstName = username.split(" ")[0];
                userNameTextView.setText(firstName);
            } else {
                userNameTextView.setText("User");
            }
        }
    }

    private void loadBase64Image(String base64, ShapeableImageView imageView) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.default_user1); // fallback
        }
    }


    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.check_account) {
                navigateToAccountFragment();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void navigateToAccountFragment() {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.homeFragment, new AccountFragment());
        transaction.addToBackStack(null);
        transaction.commit();

        if (getActivity() != null) {
            BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.nav_view);
            bottomNavigationView.setSelectedItemId(R.id.navigation_account);
        }
    }
}