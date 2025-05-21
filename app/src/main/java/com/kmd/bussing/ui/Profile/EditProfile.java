package com.kmd.bussing.ui.Profile;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.set_up_account.SignIn;

public class EditProfile extends AppCompatActivity {

    ShapeableImageView userProfile;
    ImageView back;
    TextView save;
    TextInputEditText userName, userEmail, userID;
    MaterialButton deleteAccountBtn;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    GoogleSignInClient googleSignInClient;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        back = findViewById(R.id.backButton);
        save = findViewById(R.id.saveButton);
        userName = findViewById(R.id.userNameTextInput);
        userEmail = findViewById(R.id.emailTextInput);
        userID = findViewById(R.id.uidTextInput);
        userProfile = findViewById(R.id.userProfile);
        deleteAccountBtn = findViewById(R.id.deleteButton);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("Users");

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Initialize Google Sign-In Client
        googleSignInClient = GoogleSignIn.getClient(EditProfile.this,
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.client_id))
                        .requestEmail()
                        .build());

        back.setOnClickListener(view -> onBackPressed());

        userProfile.setOnClickListener(v -> showPopupMenu(v));

        deleteAccountBtn.setOnClickListener(v -> deleteAccount());

        // Check if the user is logged in and display their info
        // Check if the user is logged in and display their info
        if (currentUser != null) {
            String fullName = currentUser.getDisplayName();
            if (fullName != null && !fullName.isEmpty()) {
                userName.setText(fullName);
            } else {
                userName.setText("User");
            }

            userEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email available");
            String userID = currentUser.getUid();
            this.userID.setText(userID);

            // 🔄 Load base64 image from Realtime Database instead of using PhotoUri
            databaseReference.child(userID).child("profileImageBase64").get()
                    .addOnSuccessListener(snapshot -> {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            loadBase64Image(base64, userProfile);
                        } else {
                            userProfile.setImageResource(R.drawable.default_user1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // fallback if DB retrieval fails
                        userProfile.setImageResource(R.drawable.default_user1);
                    });

        } else {
            userName.setText("Not logged in");
            userEmail.setText("");
            userProfile.setImageResource(R.drawable.default_user1);
        }

        // Save button functionality
        save.setOnClickListener(v -> showSaveConfirmationDialog());
    }

    private void deleteAccount() {
        // Ask the user for their password to reauthenticate
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        passwordInput.setPadding(62, 62, 62, 62);

        // a dialog asking for the password
        new AlertDialog.Builder(EditProfile.this)
                .setTitle("Reauthenticate to Delete Account")
                .setMessage("Please enter your password to confirm account deletion.")
                .setView(passwordInput)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(EditProfile.this, "Password cannot be empty!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Step 3: Reauthenticate the user
                        reauthenticateAndDeleteAccount(password);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Reauthenticate the user with the provided password
    private void reauthenticateAndDeleteAccount(String password) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Create a credential with the password
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            //Reauthenticate the user with the provided credentials
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Reauthentication successful, proceed to delete the account
                            confirmAccountDeletion();
                        } else {
                            // Reauthentication failed
                            Toast.makeText(EditProfile.this, "Authentication failed. Incorrect password.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Confirm account deletion and proceed with it
    private void confirmAccountDeletion() {
        new AlertDialog.Builder(EditProfile.this)
                .setTitle("Delete Account?")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentUser != null) {
                        progressDialog.setMessage("Deleting account...");
                        progressDialog.show(); // Show progress dialog

                        String userId = currentUser.getUid();
                        databaseReference.child(userId).removeValue()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        currentUser.delete()
                                                .addOnCompleteListener(task1 -> {
                                                    progressDialog.dismiss(); // Dismiss progress dialog
                                                    if (task1.isSuccessful()) {
                                                        Toast.makeText(EditProfile.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                        auth.signOut();
                                                        startActivity(new Intent(EditProfile.this, SignIn.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(EditProfile.this, "Failed to delete account from Firebase Authentication", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditProfile.this, "Failed to remove user data from the database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPopupMenu(View view) {
        // Create a PopupMenu and link it to the profile image
        PopupMenu popupMenu = new PopupMenu(EditProfile.this, view);


        // Inflate the menu from a resource file
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.change_profile_menu, popupMenu.getMenu());

        // Set listener for menu items
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.change_profile) {
                // Show the profile picture in full-screen (can open new activity for viewing image)
                openFileChooser();
            }
            return true;
        });

        popupMenu.show();
    }

    // Open file chooser for picking a new image
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Handle the result from the image chooser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            // Update profile image in Realtime Database
            updateProfileImage();
        }
    }

    private void loadBase64Image(String base64, ImageView imageView) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateProfileImage() {
        if (imageUri != null) {
            try {
                // Convert image to Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Compress and convert to Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

                // 3. Save Base64 string to Realtime Database
                String userId = currentUser.getUid();
                databaseReference.child(userId).child("profileImageBase64").setValue(base64Image)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 4. Show in UI immediately
                                userProfile.setImageBitmap(bitmap);

                                // Broadcast change (if used by other fragments)
                                Intent intent = new Intent("com.example.PROFILE_UPDATED");
                                sendBroadcast(intent);

                                Toast.makeText(EditProfile.this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(EditProfile.this, "Failed to update profile image", Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Show the profile picture in full-screen
    private void showProfileImage() {
        // You can implement an activity to show the image in full screen, or a dialog
        Toast.makeText(EditProfile.this, "Showing Profile Image", Toast.LENGTH_SHORT).show();
    }

    // Show a confirmation dialog before saving the username
    private void showSaveConfirmationDialog() {
        // Show an AlertDialog to confirm the changes
        new AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Are you sure you want to save the changes to your profile?")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Update the username in Firebase
                        updateUsername();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Update the username in Firebase Authentication and Realtime Database
    private void updateUsername() {
        String newUsername = userName.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(EditProfile.this, "Username cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Saving changes...");
        progressDialog.show();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = currentUser.getUid();
                        databaseReference.child(userId).child("name").setValue(newUsername)
                                .addOnCompleteListener(task1 -> {
                                    progressDialog.dismiss();
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(EditProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(EditProfile.this, "Failed to update profile in database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfile.this, "Failed to update username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
