package com.kmd.bussing.ui.ticket;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.kmd.bussing.R;

public class GenerateTicket extends AppCompatActivity {

    ImageView back;
    private ImageView qrCodeImage;
    private TextView fromText, toText, userNameText, bookingTime, bookingDate, passenger, discountText, fareText, ticketNo, subtotal;
    private MaterialButton downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generate_ticket);

        // Initialize UI elements
        qrCodeImage = findViewById(R.id.qrCode);
        fromText = findViewById(R.id.fromTicket);
        toText = findViewById(R.id.toWhereTicket);
        userNameText = findViewById(R.id.passengerName);
        bookingTime = findViewById(R.id.bookingTime);
        bookingDate = findViewById(R.id.bookingDate);
        passenger = findViewById(R.id.passenger);
        discountText = findViewById(R.id.discount);
        fareText = findViewById(R.id.total);
        ticketNo = findViewById(R.id.ticketNo);
        downloadButton = findViewById(R.id.download);
        subtotal = findViewById(R.id.subtotal);

        // Retrieve data from Intent
        String from = getIntent().getStringExtra("from");
        String to = getIntent().getStringExtra("to");
        String userName = getIntent().getStringExtra("userName");
        String date = getIntent().getStringExtra("date");
        String dateTime = getIntent().getStringExtra("dateTime");
        String passengerType = getIntent().getStringExtra("passengerType");
        String discountAmount = getIntent().getStringExtra("discountAmount");
        String finalFare = getIntent().getStringExtra("price");
        String ticketNumber = getIntent().getStringExtra("ticketNumber");
        String ticketDetails = getIntent().getStringExtra("ticketDetails");
        String basePriceStr = getIntent().getStringExtra("subtotal");

        // Fallback for basePrice (if null or invalid)
        double basePrice = 120.0; // Default base price
        if (basePriceStr != null) {
            try {
                basePrice = Double.parseDouble(basePriceStr);
            } catch (NumberFormatException e) {
                Log.e("GenerateTicket", "Invalid subtotal received", e);
            }
        }

        // Format the date properly
        String formattedDate = formatDate(date);

        // Format passenger type (capitalize first letter)
        String formattedPassengerType = passengerType != null && !passengerType.isEmpty()
                ? passengerType.substring(0, 1).toUpperCase() + passengerType.substring(1)
                : "Passenger";

        // Set data in UI
        fromText.setText(from);
        toText.setText(to);
        userNameText.setText(userName);
        bookingDate.setText(formattedDate);
        bookingTime.setText(dateTime);
        passenger.setText(formattedPassengerType);
        discountText.setText("₱" + discountAmount);
        fareText.setText("₱" + finalFare);
        ticketNo.setText(ticketNumber);
        subtotal.setText(String.format(Locale.getDefault(), "₱%.2f", basePrice));

        // Generate QR Code
        generateQRCode(ticketDetails);

        // Adjust UI colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#143ac1"));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Handle button actions
        back = findViewById(R.id.backButton);
        back.setOnClickListener(view -> onBackPressed());
        downloadButton.setOnClickListener(v -> downloadTicket());
    }


    private void downloadTicket() {
        FrameLayout ticketLayout = findViewById(R.id.main);

        ImageView backButton = findViewById(R.id.backButton);
        MaterialButton downloadButton = findViewById(R.id.download);
        TextView ticketDetailsText = findViewById(R.id.ticketDetailsText);

        // Hide elements before capturing
        backButton.setVisibility(View.GONE);
        downloadButton.setVisibility(View.GONE);
        ticketDetailsText.setVisibility(View.GONE);

        int originalColor = ((ColorDrawable) ticketLayout.getBackground()).getColor();

        // Change background color to #143ac1
        ticketLayout.setBackgroundColor(Color.parseColor("#143ac1"));

        Bitmap bitmap = Bitmap.createBitmap(ticketLayout.getWidth(), ticketLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        ticketLayout.draw(canvas);

        // Restore original background color
        ticketLayout.setBackgroundColor(originalColor);

        // Restore visibility after capturing
        backButton.setVisibility(View.VISIBLE);
        downloadButton.setVisibility(View.VISIBLE);
        ticketDetailsText.setVisibility(View.VISIBLE);

        // Save bitmap as an image
        saveBitmap(bitmap);
    }

    private void saveBitmap(Bitmap bitmap) {
        String fileName = "BusTicket_" + System.currentTimeMillis() + ".png";

        OutputStream outputStream;
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save using MediaStore (for Android 10+)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BusTickets");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                    Toast.makeText(this, "Ticket saved to Gallery!", Toast.LENGTH_SHORT).show();

                } else {
                    // Save using FileOutputStream (for older Android versions)
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BusTickets");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File file = new File(directory, fileName);
                    outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    Toast.makeText(this, "Ticket saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
            }

        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save ticket", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to format date from "MM/dd/yyyy" to "ddMMMyy"
    private String formatDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("ddMMMyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            e.printStackTrace();
            return date;
        }
    }

    // Method to generate QR Code
    private void generateQRCode(String text) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            BitMatrix bitMatrix = new com.google.zxing.MultiFormatWriter().encode(
                    text, BarcodeFormat.QR_CODE, 400, 400);
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCodeImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
