package com.night.SkyNote.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.night.SkyNote.R;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    // Activity Result Launcher for Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleGoogleSignInResult(task);
                } else {
                    Log.w("LoginActivity", "Google Sign-In failed");
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Google Sign-In Button
        Button googleButton = findViewById(R.id.btn_google);
        googleButton.setOnClickListener(v -> signInWithGoogle());

        // Twitter Sign-In Button
        Button twitterButton = findViewById(R.id.btn_twitter);
        twitterButton.setOnClickListener(v -> signInWithTwitter());
    }

    // Google Sign-In
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w("LoginActivity", "Google sign-in failed", e);
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("LoginActivity", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("LoginActivity", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            navigateToMainActivity();
                        } else {
                            Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Twitter Sign-In
    private void signInWithTwitter() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");

        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // Handle pending sign-in
            pendingResultTask
                    .addOnSuccessListener(authResult -> navigateToMainActivity())
                    .addOnFailureListener(e -> {
                        Log.w("LoginActivity", "Twitter sign-in failed", e);
                        Toast.makeText(LoginActivity.this, "Twitter sign-in failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Start new sign-in
            mAuth.startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener(authResult -> navigateToMainActivity())
                    .addOnFailureListener(e -> {
                        Log.w("LoginActivity", "Twitter sign-in failed", e);
                        Toast.makeText(LoginActivity.this, "Twitter sign-in failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Navigate to MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
