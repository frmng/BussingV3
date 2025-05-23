package com.kmd.bussing.ui.set_up_account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import com.kmd.bussing.R;
import com.kmd.bussing.MainActivity;

public class SignIn extends AppCompatActivity {

    FirebaseAuth auth;
    GoogleSignInClient gsc;
    FirebaseDatabase database;
    TextView toSignUp;
    TextInputEditText email, password;
    Button gsignInBtn, loginBtn;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.emailTextInput);
        password = findViewById(R.id.passwordTextInput);
        gsignInBtn = findViewById(R.id.continueWithGoogleBtn);
        loginBtn = findViewById(R.id.loginButton);
        toSignUp = findViewById(R.id.toSignUp);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("216430528785-3tk2lmml9ikn7j6un9bjvhlp1lgevi3u.apps.googleusercontent.com")
                .requestEmail().build();

        gsc = GoogleSignIn.getClient(this, gso);

        gsignInBtn.setOnClickListener(view -> gSignIn());

        loginBtn.setOnClickListener(view -> {
            String user = email.getText().toString();
            String pass = password.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(SignIn.this, "Email and Password cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                progressDialog.show();
                new SimulateNetworkSpeed().execute(user, pass);
            }
        });

        toSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(SignIn.this, SignUp.class);
            startActivity(intent);
            finish();
        });
    }

    int RC_SIGN_IN = 40;

    private void gSignIn() {
        Intent intent = gsc.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            if (task != null) {
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    auth(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(SignIn.this, "Sign in was cancelled.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void auth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            ensureWalletExists(user.getUid());
                            checkAndCreateUser(user);  // ✅ Call this method
                        }

                        Intent intent = new Intent(SignIn.this, MainActivity.class);
                        startActivity(intent);

                    } else {
                        Toast.makeText(SignIn.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUser(FirebaseUser firebaseUser) {
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        User users = new User();
                        users.setUserId(firebaseUser.getUid());
                        users.setName(firebaseUser.getDisplayName());
                        users.setProfile(firebaseUser.getPhotoUrl() != null ?
                                firebaseUser.getPhotoUrl().toString() : "");
                        users.setEmail(firebaseUser.getEmail());

                        // Get current date and time in format: yyyy-MM-dd HH:mm:ss
                        String createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date());
                        users.setCreatedAt(createdAt);

                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("Users")
                                .child(firebaseUser.getUid())
                                .setValue(users);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SignIn", "Failed to check/create user", e);
                });
    }


    private void ensureWalletExists(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userWalletRef = db.collection("UserWalletsCollection").document(userId);

        userWalletRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                createWalletForUser(userId);
            } else {
                Log.d("Firestore", "Wallet already exists for user: " + userId);
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Failed to check wallet existence", e));
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

    // Simulate delay based on internet speed
    private class SimulateNetworkSpeed extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            long delay = measureNetworkSpeed();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(String user) {
            auth.signInWithEmailAndPassword(user, password.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        progressDialog.dismiss();
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            ensureWalletExists(firebaseUser.getUid());
                        }
                        Toast.makeText(SignIn.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignIn.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(SignIn.this, "Invalid email or password!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private long measureNetworkSpeed() {
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            InputStream input = connection.getInputStream();
            input.close();
            long elapsedTime = System.currentTimeMillis() - startTime;

            return elapsedTime < 300 ? 1000 : elapsedTime < 1000 ? 2000 : 4000;
        } catch (Exception e) {
            return 3000;
        }
    }
}
