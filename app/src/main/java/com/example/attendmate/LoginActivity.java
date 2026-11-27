package com.example.attendmate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends Activity {

    private AutoCompleteTextView emailInput;
    private EditText passwordInput;
    private ImageButton showPasswordIcon;

    private FirebaseAuth auth;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            openDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        Button loginButton = findViewById(R.id.loginButton);

        TextView registerLink = findViewById(R.id.registerLink);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        showPasswordIcon = findViewById(R.id.showPasswordIcon);

        // White text for dark blue input fields
        emailInput.setTextColor(0xFFFFFFFF);
        passwordInput.setTextColor(0xFFFFFFFF);

        emailInput.setHintTextColor(0xFFB8C9E2);
        passwordInput.setHintTextColor(0xFFB8C9E2);

        // Show and hide password using eye icon
        showPasswordIcon.setOnClickListener(v -> {

            int cursorPosition = passwordInput.getSelectionStart();

            if (!isPasswordVisible) {

                passwordInput.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());

                showPasswordIcon.setImageResource(
                        R.drawable.ic_visibility_off);

                showPasswordIcon.setContentDescription("Hide password");

                isPasswordVisible = true;

            } else {

                passwordInput.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());

                showPasswordIcon.setImageResource(
                        R.drawable.ic_visibility);

                showPasswordIcon.setContentDescription("Show password");

                isPasswordVisible = false;
            }

            passwordInput.setSelection(
                    Math.max(0, Math.min(cursorPosition, passwordInput.length())));
        });

        // Login button
        loginButton.setOnClickListener(v -> login());

        // Register link
        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Forgot password link
        forgotPasswordText.setOnClickListener(v -> resetPassword());
    }

    private void login() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Please enter your email");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Please enter your password");
            passwordInput.requestFocus();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Login successful",
                                Toast.LENGTH_SHORT).show();

                        openDashboard();

                    } else {

                        Toast.makeText(this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Login failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Firebase password reset
    private void resetPassword() {

        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {

            emailInput.setError("Please enter your email address");
            emailInput.requestFocus();

            Toast.makeText(this,
                    "Enter your registered email address first",
                    Toast.LENGTH_LONG).show();

            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Password reset link sent. Please check your email.",
                                Toast.LENGTH_LONG).show();

                    } else {

                        Toast.makeText(this,
                                "Unable to send reset email. Please check your email address.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openDashboard() {

        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
