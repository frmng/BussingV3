package com.kmd.bussing.ui.set_up_account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.kmd.bussing.R;

public class SignUp extends AppCompatActivity {

    TextInputEditText email, password, confirmPassword, name;
    Button signup;
    FirebaseAuth auth;
    TextView toSignIn;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        toSignIn = findViewById(R.id.toLogIn);
        email = findViewById(R.id.emailTextInput);
        name = findViewById(R.id.nameTextInput);
        password = findViewById(R.id.passwordTextInput);
        confirmPassword = findViewById(R.id.confirmPassTextInput);
        signup = findViewById(R.id.signupButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        signup.setOnClickListener(view -> {
            String user = email.getText().toString();
            String pass = password.getText().toString();
            String confirmPass = confirmPassword.getText().toString();
            String userName = name.getText().toString();

            if (user.isEmpty() || userName.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(SignUp.this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(user).matches()) {
                email.setError("Please enter a valid email address!");
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(SignUp.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                password.setError("Password should be at least 6 characters long!");
                return;
            }

            progressDialog.show();

            auth.createUserWithEmailAndPassword(user, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            saveUserData(firebaseUser, user);

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (!profileTask.isSuccessful()) {
                                            Toast.makeText(SignUp.this, "Profile update failed, but account was created", Toast.LENGTH_SHORT).show();
                                        }
                                        progressDialog.dismiss();
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(SignUp.this, "Signup Unsuccessful: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        toSignIn.setOnClickListener(view -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void saveUserData(FirebaseUser firebaseUser, String email) {
        String userId = firebaseUser.getUid();
        String userName = name.getText().toString();

        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH: mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("id", userId);
        userData.put("name", userName);
        userData.put("createdAt", createdAt);


        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        databaseRef.child("Users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        createWalletForUser(userId);
                        Toast.makeText(SignUp.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), SignIn.class));
                        finish();
                    } else {
                        Toast.makeText(SignUp.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createWalletForUser(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userWalletRef = db.collection("UserWalletsCollection").document(userId);

        Map<String, Object> walletData = new HashMap<>();
        walletData.put("balance", 0.0);

        userWalletRef.set(walletData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Wallet created for user: " + userId))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to create wallet", e));
    }
}
