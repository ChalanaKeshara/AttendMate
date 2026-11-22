package com.example.attendmate;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class LoginActivity extends Activity {

    private AutoCompleteTextView emailInput;
    private EditText passwordInput;

    private CheckBox showPassword;
    private CheckBox rememberMe;

    private FirebaseAuth auth;
    private SharedPreferences preferences;

    private static final String PREF_NAME = "AttendMatePrefs";
    private static final String KEY_EMAIL = "remembered_email";

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

        showPassword = findViewById(R.id.showPasswordCheck);
        rememberMe = findViewById(R.id.rememberMeCheck);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // White input text for dark blue fields
        emailInput.setTextColor(0xFFFFFFFF);
        passwordInput.setTextColor(0xFFFFFFFF);

        emailInput.setHintTextColor(0xFFB8C9E2);
        passwordInput.setHintTextColor(0xFFB8C9E2);

        loadSavedEmail();

        // Show or hide password
        showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {

            int cursorPosition = passwordInput.getSelectionStart();

            if (isChecked) {
                passwordInput.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
            } else {
                passwordInput.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
            }

            passwordInput.setSelection(
                    Math.max(0, Math.min(cursorPosition, passwordInput.length())));
        });

        // Remember me
        rememberMe.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                preferences.edit().remove(KEY_EMAIL).apply();
            }
        });

        loginButton.setOnClickListener(v -> login());

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loadSavedEmail() {

        String savedEmail = preferences.getString(KEY_EMAIL, "");

        if (!savedEmail.isEmpty()) {

            rememberMe.setChecked(true);
            emailInput.setText(savedEmail);

            ArrayList<String> emailSuggestions = new ArrayList<>();
            emailSuggestions.add(savedEmail);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    emailSuggestions
            );

            emailInput.setAdapter(adapter);
            emailInput.setThreshold(1);

            emailInput.setOnClickListener(v -> emailInput.showDropDown());
        }
    }

    private void login() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        if (rememberMe.isChecked()) {
                            preferences.edit()
                                    .putString(KEY_EMAIL, email)
                                    .apply();
                        } else {
                            preferences.edit()
                                    .remove(KEY_EMAIL)
                                    .apply();
                        }

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
