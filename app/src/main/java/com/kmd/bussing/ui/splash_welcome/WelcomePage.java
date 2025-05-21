package com.kmd.bussing.ui.splash_welcome;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.kmd.bussing.MainActivity;
import com.kmd.bussing.R;
import com.kmd.bussing.ui.set_up_account.SignIn;
import com.kmd.bussing.ui.set_up_account.SignUp;

public class WelcomePage extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();

        // Check if the user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If the user is signed in, navigate to MainActivity directly
            navigateToMainActivity();
        } else {
            // If the user is not signed in, show the welcome page
            setContentView(R.layout.activity_welcome_page);

            findViewById(R.id.signInButton).setOnClickListener(view -> {
                Intent intent = new Intent(WelcomePage.this, SignIn.class);
                startActivity(intent);
                finish();
            });

            findViewById(R.id.registerButton).setOnClickListener(view -> {
                Intent intent = new Intent(WelcomePage.this, SignUp.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow going back to the welcome screen once the user proceeds
        finish();
        super.onBackPressed();
    }

    // Method to navigate to MainActivity
    public void navigateToMainActivity() {
        // Use the flags to clear the back stack and prevent going back to WelcomePage
        Intent intent = new Intent(WelcomePage.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}