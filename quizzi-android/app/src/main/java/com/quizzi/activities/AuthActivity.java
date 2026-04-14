package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.quizzi.R;
import com.quizzi.network.ApiClient;

import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout welcomeView, loginView, signupView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        welcomeView = findViewById(R.id.welcomeView);
        loginView = findViewById(R.id.loginView);
        signupView = findViewById(R.id.signupView);

        findViewById(R.id.btnLogin).setOnClickListener(v -> showView(loginView));
        findViewById(R.id.btnSignup).setOnClickListener(v -> showView(signupView));
        findViewById(R.id.btnGuest).setOnClickListener(v -> goToJoinGame("Guest", "", 0));

        findViewById(R.id.loginBack).setOnClickListener(v -> showView(welcomeView));
        findViewById(R.id.signupBack).setOnClickListener(v -> showView(welcomeView));
        findViewById(R.id.loginToSignup).setOnClickListener(v -> showView(signupView));
        findViewById(R.id.signupToLogin).setOnClickListener(v -> showView(loginView));

        setupLogin();
        setupSignup();
    }

    private void showView(LinearLayout target) {
        welcomeView.setVisibility(target == welcomeView ? View.VISIBLE : View.GONE);
        loginView.setVisibility(target == loginView ? View.VISIBLE : View.GONE);
        signupView.setVisibility(target == signupView ? View.VISIBLE : View.GONE);
    }

    private void setupLogin() {
        EditText emailField = findViewById(R.id.loginEmail);
        EditText passwordField = findViewById(R.id.loginPassword);
        TextView errorText = findViewById(R.id.loginError);
        MaterialButton loginBtn = findViewById(R.id.btnDoLogin);

        loginBtn.setOnClickListener(v -> {
            String username = emailField.getText().toString().trim();
            String password = passwordField.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                showError(errorText, "Please enter email and password");
                return;
            }

            loginBtn.setEnabled(false);
            errorText.setVisibility(View.GONE);

            new Thread(() -> {
                try {
                    String json = "{\"username\":\"" + escapeJson(username)
                            + "\",\"password\":\"" + escapeJson(password) + "\"}";
                    String resp = ApiClient.postJson("/api/login", json);
                    JSONObject result = new JSONObject(resp);

                    if ("ok".equals(result.getString("status"))) {
                        String displayName = result.optString("displayName", username);
                        int userId = result.optInt("userId", 0);
                        runOnUiThread(() -> goToJoinGame(displayName, username, userId));
                    } else {
                        String msg = result.optString("message", "Login failed");
                        runOnUiThread(() -> showError(errorText, msg));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> showError(errorText, "Connection error: " + e.getMessage()));
                }
                runOnUiThread(() -> loginBtn.setEnabled(true));
            }).start();
        });
    }

    private void setupSignup() {
        EditText displayNameField = findViewById(R.id.signupDisplayName);
        EditText emailField = findViewById(R.id.signupEmail);
        EditText passwordField = findViewById(R.id.signupPassword);
        TextView errorText = findViewById(R.id.signupError);
        MaterialButton signupBtn = findViewById(R.id.btnDoSignup);

        signupBtn.setOnClickListener(v -> {
            String displayName = displayNameField.getText().toString().trim();
            String username = emailField.getText().toString().trim();
            String password = passwordField.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                showError(errorText, "Please enter email and password");
                return;
            }

            signupBtn.setEnabled(false);
            errorText.setVisibility(View.GONE);

            new Thread(() -> {
                try {
                    String json = "{\"username\":\"" + escapeJson(username)
                            + "\",\"password\":\"" + escapeJson(password)
                            + "\",\"displayName\":\"" + escapeJson(displayName.isEmpty() ? username : displayName)
                            + "\"}";
                    String resp = ApiClient.postJson("/api/register", json);
                    JSONObject result = new JSONObject(resp);

                    if ("ok".equals(result.getString("status"))) {
                        String name = result.optString("displayName", username);
                        int userId = result.optInt("userId", 0);
                        runOnUiThread(() -> goToJoinGame(name, username, userId));
                    } else {
                        String msg = result.optString("message", "Registration failed");
                        runOnUiThread(() -> showError(errorText, msg));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> showError(errorText, "Connection error: " + e.getMessage()));
                }
                runOnUiThread(() -> signupBtn.setEnabled(true));
            }).start();
        });
    }

    private void goToJoinGame(String displayName, String username, int userId) {
        Intent intent = new Intent(this, JoinGameActivity.class);
        intent.putExtra("displayName", displayName);
        intent.putExtra("username", username);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void showError(TextView errorText, String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
