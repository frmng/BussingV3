package com.kmd.bussing.ui.ticket;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.icu.util.Calendar;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.kmd.bussing.R;

public class TicketFragment extends Fragment {

    private AutoCompleteTextView departureDropdown, arrivalDropdown, passengersDropdown;
    private EditText dateInput;
    private MaterialButton qrCodeButton;
    private FirebaseFirestore db;
    private ImageView switchRouteButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket, container, false);

        departureDropdown = view.findViewById(R.id.departureDropDown);
        arrivalDropdown = view.findViewById(R.id.arrivalDropDown);
        passengersDropdown = view.findViewById(R.id.passengersDropDown);
        dateInput = view.findViewById(R.id.dateInput);
        qrCodeButton = view.findViewById(R.id.qrCodeButton);
        switchRouteButton = view.findViewById(R.id.switchRoute);

        db = FirebaseFirestore.getInstance();

        Bundle bundle = getArguments();
        String fromLocation = bundle != null ? bundle.getString("from", "") : "";
        String toLocation = bundle != null ? bundle.getString("to", "") : "";

        loadRoutesFromFirestore(fromLocation, toLocation);

        ArrayAdapter<String> passengersAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.passengers)
        );
        passengersDropdown.setAdapter(passengersAdapter);

        dateInput.setOnClickListener(v -> showDatePickerDialog());

        switchRouteButton.setOnClickListener(v -> {
            String departure = departureDropdown.getText().toString();
            String arrival = arrivalDropdown.getText().toString();

            // Swap route
            departureDropdown.setText(arrival, false);
            arrivalDropdown.setText(departure, false);

            // Rotate icon
            switchRouteButton.animate()
                    .rotationBy(180f)
                    .setDuration(300)
                    .start();
        });


        qrCodeButton.setOnClickListener(v -> {
            if (!isFormValid()) {
                Snackbar.make(v, "Please complete all required fields before proceeding.", Snackbar.LENGTH_LONG).show();
                return;
            }

            String fromWhere = departureDropdown.getText().toString();
            String toWhere = arrivalDropdown.getText().toString();

            if (fromWhere.equalsIgnoreCase(toWhere)) {
                Snackbar.make(v, "Departure and Arrival locations cannot be the same.", Snackbar.LENGTH_LONG).show();
                return;
            }

            String from = departureDropdown.getText().toString();
            String to = arrivalDropdown.getText().toString();
            String date = dateInput.getText().toString();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            String ticketNumber = generateTicketNumber();
            String passengerType = passengersDropdown.getText().toString().trim().toLowerCase();

            // Determine base price (subtotal)
            double basePrice = (from.equalsIgnoreCase("Bulakan") && to.equalsIgnoreCase("Cubao")) ? 105.0 : 120.0;
            double discountRate = 0.0;

            switch (passengerType) {
                case "student":
                    discountRate = 0.2;
                    break;
                case "senior":
                    discountRate = 0.3;
                    break;
                case "pwd":
                    discountRate = 0.4;
                    break;
            }

            double discountAmount = basePrice * discountRate;
            double finalPrice = basePrice - discountAmount;

            showPaymentDialog(finalPrice, () -> {
                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage("Generating QR Code...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String formattedDateTime = sdf.format(new Date());

                    String formattedPrice = String.format(Locale.getDefault(), "%.2f", finalPrice);
                    String formattedDiscount = String.format(Locale.getDefault(), "%.2f", discountAmount);
                    String formattedSubtotal = String.format(Locale.getDefault(), "%.2f", basePrice);

                    String ticketDetails = "From: " + from + "\nTo: " + to + "\nUser: " + userName +
                            "\nDate: " + date + "\nTime: " + formattedDateTime +
                            "\nPassenger: " + passengerType + "\nDiscount: ₱" + formattedDiscount +
                            "\nSubtotal: ₱" + formattedSubtotal +
                            "\nTotal Price: ₱" + formattedPrice +
                            "\nTicket Number: " + ticketNumber;

                    Intent intent = new Intent(getActivity(), GenerateTicket.class);
                    intent.putExtra("from", from);
                    intent.putExtra("to", to);
                    intent.putExtra("userName", userName);
                    intent.putExtra("date", date);
                    intent.putExtra("dateTime", formattedDateTime);
                    intent.putExtra("passengerType", passengerType);
                    intent.putExtra("discountAmount", formattedDiscount);
                    intent.putExtra("price", formattedPrice);
                    intent.putExtra("subtotal", formattedSubtotal);
                    intent.putExtra("ticketDetails", ticketDetails);
                    intent.putExtra("ticketNumber", ticketNumber);

                    saveTicketToFirestore(from, to, userName, date, formattedDateTime, passengerType, formattedDiscount, formattedPrice, ticketNumber, formattedSubtotal);

                    passengersDropdown.setText("");

                    progressDialog.dismiss();

                    startActivity(intent);
                }, 2000);
            });
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resetPassengerDropdown();
    }

    private void resetPassengerDropdown() {
        ArrayAdapter<String> passengersAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.passengers)
        );
        passengersDropdown.setAdapter(passengersAdapter);
    }

    private void saveTicketToFirestore(String from, String to, String userName, String date, String dateTime,
                                       String passengerType, String discountAmount, String finalPrice, String ticketNumber, String subtotal) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("createdAt", Timestamp.now());
        ticketData.put("from", from);
        ticketData.put("to", to);
        ticketData.put("userName", userName);
        ticketData.put("bookingDate", date);
        ticketData.put("bookingTime", dateTime);
        ticketData.put("passenger", passengerType);
        ticketData.put("discount", discountAmount);
        ticketData.put("subtotal", subtotal);
        ticketData.put("price", finalPrice);
        ticketData.put("ticketCode", ticketNumber);
        ticketData.put("uid", userId);

        FirebaseFirestore.getInstance()
                .collection("TicketGeneratedCollection")
                .add(ticketData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Ticket saved: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving ticket", e));
    }

    private void showPaymentDialog(double amountToPay, Runnable onPaymentSuccess) {
        if (!isAdded() || getActivity() == null) {
            Log.e("PaymentDialog", "Fragment is not attached, cannot show dialog.");
            return;
        }

        getActivity().runOnUiThread(() -> {
            String from = departureDropdown.getText().toString();
            String to = arrivalDropdown.getText().toString();
            String passengerType = passengersDropdown.getText().toString().trim().toLowerCase();
            double basePrice = (from.equalsIgnoreCase("Bulakan") && to.equalsIgnoreCase("Cubao")) ? 105.0 : 120.0;

            double discountRate = 0.0;
            switch (passengerType) {
                case "student":
                    discountRate = 0.2;
                    break;
                case "senior":
                    discountRate = 0.3;
                    break;
                case "pwd":
                    discountRate = 0.4;
                    break;
            }

            double discountAmount = basePrice * discountRate;
            double totalPrice = basePrice - discountAmount;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Ticket Invoice");

            String invoiceMessage = String.format(
                    Locale.getDefault(),
                    "Route: %s to %s\nPassenger: %s\n\nSubtotal: ₱%.2f\nDiscount: ₱%.2f\n\nTotal: ₱%.2f",
                    from, to, passengerType.isEmpty() ? "Regular" : passengerType,
                    basePrice, discountAmount, totalPrice
            );

            builder.setMessage(invoiceMessage);

            builder.setPositiveButton("Confirm & Pay", (dialog, which) -> {
                processPayment(totalPrice, onPaymentSuccess);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();

                // clear dropdown focus and reset selection
                passengersDropdown.dismissDropDown();
                passengersDropdown.postDelayed(() -> {
                    passengersDropdown.showDropDown();
                }, 100);
            });


            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void processPayment(double amountToPay, Runnable onPaymentSuccess) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userWalletRef = db.collection("UserWalletsCollection").document(userId);

        String from = departureDropdown.getText().toString();
        String to = arrivalDropdown.getText().toString();

        db.collection("ScheduleDocumentsCollection")
                .whereEqualTo("from", from)
                .whereEqualTo("to", to)
                .get()
                .addOnSuccessListener(scheduleSnapshots -> {
                    if (scheduleSnapshots.isEmpty()) {
                        Snackbar.make(qrCodeButton, "No matching schedule found.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot scheduleDoc = scheduleSnapshots.getDocuments().get(0);
                    DocumentReference scheduleRef = scheduleDoc.getReference();

                    Long availableSeats;
                    try {
                        Object seatsObj = scheduleDoc.get("availableSeats");
                        if (seatsObj instanceof Number) {
                            availableSeats = ((Number) seatsObj).longValue();
                        } else if (seatsObj instanceof String) {
                            availableSeats = Long.parseLong((String) seatsObj);
                        } else {
                            Snackbar.make(qrCodeButton, "Invalid seat format.", Snackbar.LENGTH_LONG).show();
                            return;
                        }
                    } catch (Exception e) {
                        Snackbar.make(qrCodeButton, "Failed to parse availableSeats.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    if (availableSeats == null || availableSeats <= 0) {
                        Snackbar.make(qrCodeButton, "No seats available on this bus.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // Run transaction to deduct seat and balance
                    db.runTransaction(transaction -> {
                        DocumentSnapshot walletSnapshot = transaction.get(userWalletRef);
                        DocumentSnapshot latestScheduleSnapshot = transaction.get(scheduleRef);

                        Double balance = walletSnapshot.getDouble("balance");

                        Long latestAvailableSeats;
                        Object latestSeatsObj = latestScheduleSnapshot.get("availableSeats");
                        if (latestSeatsObj instanceof Number) {
                            latestAvailableSeats = ((Number) latestSeatsObj).longValue();
                        } else if (latestSeatsObj instanceof String) {
                            latestAvailableSeats = Long.parseLong((String) latestSeatsObj);
                        } else {
                            throw new FirebaseFirestoreException("Invalid seat format", FirebaseFirestoreException.Code.ABORTED);
                        }

                        if (balance == null || balance < amountToPay) {
                            throw new FirebaseFirestoreException("Insufficient balance", FirebaseFirestoreException.Code.ABORTED);
                        }

                        if (latestAvailableSeats == null || latestAvailableSeats <= 0) {
                            throw new FirebaseFirestoreException("No seats left", FirebaseFirestoreException.Code.ABORTED);
                        }

                        // Deduct from wallet and update seat count
                        transaction.update(userWalletRef, "balance", balance - amountToPay);
                        transaction.update(scheduleRef, "availableSeats", latestAvailableSeats - 1);

                        return null;
                    }).addOnSuccessListener(unused -> {
                        Log.d("Firestore", "Payment and seat deduction successful.");
                        deductBalance(amountToPay);
                        savePaymentRecord(userId, amountToPay);
                        saveWalletTransaction(userId, amountToPay);
                        Snackbar.make(qrCodeButton, "Payment successful! Ticket booked and seat reserved.", Snackbar.LENGTH_LONG).show();
                        onPaymentSuccess.run();
                    }).addOnFailureListener(e -> {
                        Log.e("Firestore", "Transaction failed: " + e.getMessage(), e);
                        Snackbar.make(qrCodeButton, "Failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });

                })
                .addOnFailureListener(e -> {
                    Snackbar.make(qrCodeButton, "Error fetching schedule.", Snackbar.LENGTH_LONG).show();
                    Log.e("Firestore", "Failed fetching schedule: ", e);
                });
    }

    private void deductBalance(double amount) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

                        Map<String, Object> transactionData = new HashMap<>();
                        transactionData.put("userActivity", "You booked a ticket from");
                        transactionData.put("transactionPrice", -amount);
                        transactionData.put("transactionTimeStamp", dateTime);
                        transactionData.put("userId", "Bussing");

                        walletRef.collection("transactions").add(transactionData)
                                .addOnSuccessListener(documentReference ->
                                        Log.d("Firestore", "Transaction stored successfully"))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Error storing transaction", e));

                        Snackbar.make(qrCodeButton, "Payment Successful! Ticket booked.", Snackbar.LENGTH_LONG).show();
                    }).addOnFailureListener(e ->
                            Snackbar.make(qrCodeButton, "Failed to deduct balance", Snackbar.LENGTH_LONG).show());
                } else {
                    Snackbar.make(qrCodeButton, "Insufficient Balance!", Snackbar.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(e ->
                Snackbar.make(qrCodeButton, "Failed to retrieve wallet balance", Snackbar.LENGTH_LONG).show());
    }


    private void saveWalletTransaction(String userId, double amountPaid) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String departure = departureDropdown.getText().toString();
        String arrival = arrivalDropdown.getText().toString();

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("userId", userId);
        transactionData.put("userActivity", "You booked a ticket from " + departure + " to " + arrival);
        transactionData.put("transactionPrice", -amountPaid);
        transactionData.put("transactionTimeStamp", dateTime);

        db.collection("UserWalletTransactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Transaction recorded successfully! ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to record transaction", e);
                });
    }



    private void savePaymentRecord(String userId, double amountPaid) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        Map<String, Object> paymentDetails = new HashMap<>();
        paymentDetails.put("userId", userId);
        paymentDetails.put("dateTime", dateTime);
        paymentDetails.put("amountPaid", amountPaid);

        db.collection("BussingPaymentsCollection")
                .add(paymentDetails)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Payment recorded successfully! ID: " + documentReference.getId());
                    Snackbar.make(qrCodeButton, "Payment recorded successfully!", Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to record payment", e);
                    Snackbar.make(qrCodeButton, "Failed to record payment.", Snackbar.LENGTH_LONG).show();
                });
    }



    private String generateTicketNumber() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
        return simpleDateFormat.format(new Date());
    }

    private void loadRoutesFromFirestore(String fromLocation, String toLocation) {
        CollectionReference scheduleRef = db.collection("ScheduleDocumentsCollection");

        scheduleRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Set<String> activeRoutes = new HashSet<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    if ("active".equalsIgnoreCase(document.getString("status"))) {
                        activeRoutes.add(document.getString("from"));
                        activeRoutes.add(document.getString("to"));
                    }
                }

                List<String> routeList = new ArrayList<>(activeRoutes);
                setDropdownAdapter(departureDropdown, routeList);
                setDropdownAdapter(arrivalDropdown, routeList);

                departureDropdown.post(() -> departureDropdown.setText(fromLocation, false));
                arrivalDropdown.post(() -> arrivalDropdown.setText(toLocation, false));

                Log.d("Firestore", "Routes loaded: " + routeList);
            } else {
                Log.e("Firestore", "Error loading routes", task.getException());
            }
        });
    }

    private void setDropdownAdapter(AutoCompleteTextView dropdown, List<String> dataList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, dataList);
        dropdown.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%d", selectedMonth + 1, selectedDay, selectedYear);
                    dateInput.setText(formattedDate);
                },
                year, month, dayOfMonth
        );

        // Disable past dates
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private boolean isFormValid() {
        return !departureDropdown.getText().toString().isEmpty()
                && !arrivalDropdown.getText().toString().isEmpty()
                && !passengersDropdown.getText().toString().isEmpty()
                && !dateInput.getText().toString().isEmpty();
    }
}
