package com.kmd.bussing.ui.wallet;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kmd.bussing.R;

public class UserWallet extends AppCompatActivity {

    private LinearLayout dropdownContainer;
    private TextView balanceText, dropdownText;
    private ImageView exit, addCash, dropdownIcon;
    private TextView sortButton;

    private DatabaseReference userWalletRef, paymentRef;
    private String userId;
    private static final double DEFAULT_BALANCE = 0.0;
    private SwipeRefreshLayout swipeRefreshLayout;

    private RecyclerView transactionsRecyclerView;
    private UserWalletAdapter userWalletAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_wallet);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userWalletAdapter = new UserWalletAdapter(this);
        transactionsRecyclerView.setAdapter(userWalletAdapter);

        db = FirebaseFirestore.getInstance();
        balanceText = findViewById(R.id.walletBalance);
        exit = findViewById(R.id.exitButton);
        addCash = findViewById(R.id.addCash);
        dropdownText = findViewById(R.id.dropdownText);
        dropdownIcon = findViewById(R.id.dropdownIcon);
        sortButton = findViewById(R.id.sortButton);

        dropdownContainer = findViewById(R.id.dropdownContainer);

        final boolean[] expanded = {false};

        userWalletAdapter.setOnExpandToggleListener(isExpanded -> {
            expanded[0] = isExpanded;

            // Animate icon
            dropdownIcon.animate()
                    .rotation(isExpanded ? 180f : 0f)
                    .setDuration(300)
                    .start();

            // Update text
            dropdownText.setText(isExpanded
                    ? "Click here to show less"
                    : "Click here to show all activities");
        });

        dropdownContainer.setOnClickListener(v -> {
            userWalletAdapter.setExpanded(!expanded[0]);
        });


        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userWalletRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("walletBalance");
        paymentRef = FirebaseDatabase.getInstance().getReference("BussingPayments");

        fetchWalletBalance();
        fetchTransactionsSeparately();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        exit.setOnClickListener(view -> onBackPressed());
        addCash.setOnClickListener(view -> showAddCashDialog());
        sortButton.setOnClickListener(v -> showSortDialog());

    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort Transactions");

        String[] options = {"This Month", "This Year", "All"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    filterTransactionsBy("month");
                    break;
                case 1:
                    filterTransactionsBy("year");
                    break;
                case 2:
                    fetchTransactionsSeparately(); // show all
                    break;
            }
        });

        builder.show();
    }

    private void filterTransactionsBy(String type) {
        if (userId == null || userId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference transactionsRef = db.collection("UserWalletsCollection")
                .document(userId)
                .collection("transactions");

        transactionsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            ArrayList<UserWalletList> filteredList = new ArrayList<>();

            String currentMonth = new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date()); // e.g., May
            String currentYear = new SimpleDateFormat("yy", Locale.getDefault()).format(new Date());   // e.g., 25

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String timeStamp = doc.getString("transactionTimeStamp");
                if (timeStamp == null) {
                    timeStamp = doc.getString("timeStamp");
                }
                if (timeStamp == null) continue;

                boolean include = false;
                if ("month".equals(type) && timeStamp.contains(currentMonth)) {
                    include = true;
                } else if ("year".equals(type) && timeStamp.contains(currentYear)) {
                    include = true;
                }

                if (include) {
                    addTransactionToList(doc, filteredList);
                }
            }

            userWalletAdapter.updateTransactions(filteredList);
            transactionsRecyclerView.post(userWalletAdapter::notifyDataSetChanged);

        }).addOnFailureListener(e ->
                Log.e("Firestore", "Error filtering transactions", e));
    }


    private void showAddCashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Cash");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount");
        input.setBackgroundResource(R.drawable.input_border);
        input.setPadding(30, 20, 30, 20);

        // Set layout parameters for margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 20, 40, 20);
        input.setLayoutParams(params);

        // Create a container layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                if (amount > 0) {
                    addCashToWallet(amount);
                } else {
                    showSnackbar("Enter a valid amount!");
                }
            } else {
                showSnackbar("Amount cannot be empty!");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void refreshData() {
        fetchWalletBalance();
        fetchTransactionsSeparately();

        // Add a delay to make the refresh smooth
        new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
    }


    private void addCashToWallet(double amount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference walletRef = db.collection("UserWalletsCollection").document(userId);

        walletRef.get().addOnSuccessListener(documentSnapshot -> {
            double currentBalance = 0.0;
            if (documentSnapshot.exists() && documentSnapshot.contains("balance")) {
                currentBalance = documentSnapshot.getDouble("balance");
            }

            double newBalance = currentBalance + amount;

            String dateTime = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault()).format(new Date());
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("amount", amount);
            transactionData.put("timeStamp", dateTime);
            transactionData.put("activity", "You cash in money from");
            transactionData.put("userId", "Bussing");

            db.runTransaction(transaction -> {
                transaction.update(walletRef, "balance", newBalance);
                walletRef.collection("transactions").add(transactionData);
                return null;

            }).addOnSuccessListener(aVoid -> {
                balanceText.setText("₱" + newBalance);
                showSnackbar("Cash added successfully!");
            }).addOnFailureListener(e -> showSnackbar("Failed to update balance"));

        }).addOnFailureListener(e -> showSnackbar("Failed to retrieve wallet balance"));
    }

    private void fetchTransactionsSeparately() {
        if (userId == null || userId.isEmpty()) {
            Log.e("Firestore", "User ID is null or empty. Cannot fetch transactions.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference transactionsRef = db.collection("UserWalletsCollection")
                .document(userId)
                .collection("transactions");

        ArrayList<UserWalletList> transactionsList = new ArrayList<>();

        transactionsRef.whereEqualTo("userActivity", "You booked a ticket from")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        addTransactionToList(doc, transactionsList);
                    }

                    fetchCashInTransactions(transactionsRef, transactionsList);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching bookings", e));
    }

    private void fetchCashInTransactions(CollectionReference transactionsRef, ArrayList<UserWalletList> transactionsList) {
        transactionsRef.whereEqualTo("activity", "You cash in money from")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        addTransactionToList(doc, transactionsList);
                    }

                    userWalletAdapter.updateTransactions(transactionsList);
                    transactionsRecyclerView.post(() -> userWalletAdapter.notifyDataSetChanged());
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching cash-in transactions", e));
    }

    private void addTransactionToList(DocumentSnapshot doc, ArrayList<UserWalletList> transactionsList) {
        if (doc.exists()) {
            String activity = doc.getString("userActivity");
            if (activity == null) {
                activity = doc.getString("activity");
            }

            Double amountObj = doc.getDouble("transactionPrice");
            if (amountObj == null) {
                amountObj = doc.getDouble("amount");
            }
            double amount = (amountObj != null) ? amountObj : 0.0;

            String timeStamp = null;
            if (doc.contains("transactionTimeStamp")) {
                timeStamp = doc.getString("transactionTimeStamp");
            } else if (doc.contains("timestamp")) {
                timeStamp = doc.getString("timestamp");
            } else if (doc.contains("timeStamp")) {
                timeStamp = doc.getString("timeStamp");
            }

            if (timeStamp == null || timeStamp.isEmpty()) {
                timeStamp = "Unknown Timestamp";
            }

            String userId = doc.getString("userId");

            transactionsList.add(new UserWalletList(activity, timeStamp, userId, amount));
        }
    }


    private void storeTransaction(double amount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String dateTime = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault()).format(new Date());

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("amount", amount);
        transactionData.put("timestamp", dateTime);
        transactionData.put("activity", "You cash in money from");
        transactionData.put("userId", "Bussing");

        db.collection("UserWalletsCollection")
                .document(userId)
                .collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Transaction saved: " + documentReference.getId());
                    showSnackbar("Transaction saved");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save transaction", e);
                    showSnackbar("Failed to save transaction");
                });
    }


    private void fetchWalletBalance() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference walletRef = db.collection("UserWalletsCollection").document(userId);

        walletRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                showSnackbar("Failed to load wallet balance");
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                double balance = documentSnapshot.getDouble("balance");
                balanceText.setText("₱" + balance);
            } else {
                walletRef.set(Collections.singletonMap("balance", DEFAULT_BALANCE))
                        .addOnSuccessListener(aVoid -> balanceText.setText("₱" + DEFAULT_BALANCE))
                        .addOnFailureListener(e -> showSnackbar("Failed to initialize wallet balance"));
            }
        });
    }


    public void deductBalance(double amount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference walletRef = db.collection("UserWalletsCollection").document(userId);

        walletRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("balance")) {
                double currentBalance = documentSnapshot.getDouble("balance");
                if (currentBalance >= amount) {
                    double newBalance = currentBalance - amount;
                    String dateTime = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault()).format(new Date());

                    db.runTransaction(transaction -> {
                        transaction.update(walletRef, "balance", newBalance);
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        // Store booking transaction separately after balance update
                        Map<String, Object> transactionData = new HashMap<>();
                        transactionData.put("amount", -amount);  // Negative for deduction
                        transactionData.put("timestamp", dateTime); // Formatted timestamp
                        transactionData.put("activity", "You booked a ticket from");
                        transactionData.put("userId", "Bussing");

                        walletRef.collection("transactions").add(transactionData)
                                .addOnSuccessListener(documentReference ->
                                        Log.d("Firestore", "Transaction stored successfully"))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Error storing transaction", e));

                        // Update UI
                        balanceText.setText("₱" + newBalance);
                        balanceText.setTextColor(Color.RED);
                        showSnackbar("Payment Successful! Ticket booked.");
                    }).addOnFailureListener(e -> showSnackbar("Failed to deduct balance"));
                } else {
                    showSnackbar("Insufficient Balance!");
                }
            }
        }).addOnFailureListener(e -> showSnackbar("Failed to retrieve wallet balance"));
    }

    private void storeTransaction(double amount, String type) {
        String transactionId = paymentRef.push().getKey();
        String dateTime = new SimpleDateFormat("hh:mm a ddMMMyy", Locale.getDefault()).format(new Date());

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("amount", amount);
        transactionData.put("timestamp", dateTime);
        transactionData.put("type", type);

        paymentRef.child(userId).child("transactions").child(transactionId).setValue(transactionData);
    }

    private void storePaymentRecord(double amount) {
        String dateTime = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault()).format(new Date());

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("userId", userId);
        paymentData.put("dateTime", dateTime);
        paymentData.put("amountPaid", amount);

        paymentRef.push().setValue(paymentData);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}