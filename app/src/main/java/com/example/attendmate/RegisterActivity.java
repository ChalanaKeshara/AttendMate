package com.example.attendmate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends Activity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        Button registerButton = findViewById(R.id.registerButton);
        TextView loginLink = findViewById(R.id.loginLink);

        CheckBox showPassword = findViewById(R.id.showPasswordCheck);
        CheckBox showConfirmPassword =
                findViewById(R.id.showConfirmPasswordCheck);

        emailInput.setTextColor(0xFFFFFFFF);
        passwordInput.setTextColor(0xFFFFFFFF);
        confirmPasswordInput.setTextColor(0xFFFFFFFF);

        showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changePasswordVisibility(passwordInput, isChecked);
        });

        showConfirmPassword.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    changePasswordVisibility(confirmPasswordInput, isChecked);
                });

        registerButton.setOnClickListener(v -> register());

        loginLink.setOnClickListener(v -> finish());
    }

    private void changePasswordVisibility(EditText field, boolean visible) {

        int cursorPosition = field.getSelectionStart();

        if (visible) {
            field.setTransformationMethod(
                    HideReturnsTransformationMethod.getInstance());
        } else {
            field.setTransformationMethod(
                    PasswordTransformationMethod.getInstance());
        }

        field.setSelection(
                Math.max(0, Math.min(cursorPosition, field.length())));
    }

    private void register() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirm = confirmPasswordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please complete all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this,
                    "Password must contain at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Registration failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
