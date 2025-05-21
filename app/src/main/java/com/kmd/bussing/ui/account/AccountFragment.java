package com.kmd.bussing.ui.account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import com.kmd.bussing.R;
import com.kmd.bussing.ui.Profile.EditProfile;
import com.kmd.bussing.ui.set_up_account.SignIn;
import com.kmd.bussing.ui.wallet.WelcomeWallet;

public class AccountFragment extends Fragment {

    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView versionText;
    private com.google.android.material.imageview.ShapeableImageView profileImageView;
    private Button signOutButton, editProfile, walletBtn;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        googleSignInClient = GoogleSignIn.getClient(getContext(),
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.client_id))
                        .requestEmail()
                        .build());

        usernameTextView = root.findViewById(R.id.username);
        emailTextView = root.findViewById(R.id.userEmail);
        profileImageView = root.findViewById(R.id.userProfile);
        signOutButton = root.findViewById(R.id.signOutButton);
        editProfile = root.findViewById(R.id.editProfileButton);
        walletBtn = root.findViewById(R.id.walletButton);
        versionText = root.findViewById(R.id.versionText);

        try {
            String versionName = getActivity()
                    .getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0)
                    .versionName;

            versionText.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionText.setText("Version N/A");
        }


        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Signing out...");
        progressDialog.setCancelable(false);

        editProfile.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), EditProfile.class);
            startActivity(intent);
        });

        walletBtn.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), WelcomeWallet.class);
            startActivity(intent);
        });

        if (currentUser != null) {
            String fullName = currentUser.getDisplayName();
            usernameTextView.setText(fullName != null && !fullName.isEmpty() ? fullName : "User");
            emailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email available");

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
            String userID = currentUser.getUid();

            databaseReference.child(userID).child("profileImageBase64").get()
                    .addOnSuccessListener(snapshot -> {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            loadBase64Image(base64, profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.default_user1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        profileImageView.setImageResource(R.drawable.default_user1);
                    });

        } else {
            usernameTextView.setText("Not logged in");
            emailTextView.setText("");
            profileImageView.setImageResource(R.drawable.default_user1);
        }

        signOutButton.setOnClickListener(v -> signOut());

        return root;
    }

    private void loadBase64Image(String base64, com.google.android.material.imageview.ShapeableImageView imageView) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.default_user1); 
        }
    }


    private void signOut() {
        progressDialog.show();
        auth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            progressDialog.dismiss();
            Intent intent = new Intent(getContext(), SignIn.class);
            startActivity(intent);
            getActivity().finish();
        });
    }
}
