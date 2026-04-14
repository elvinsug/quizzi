package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.quizzi.R;
import com.quizzi.network.ApiClient;
import com.quizzi.utils.QRScannerHelper;

import org.json.JSONObject;

public class JoinGameActivity extends AppCompatActivity {

    private EditText pinInput;
    private MaterialButton enterBtn;
    private TextView errorText;
    private LinearLayout pinSection, qrSection;

    private final ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), this::handleQRResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        pinInput = findViewById(R.id.pinInput);
        enterBtn = findViewById(R.id.enterBtn);
        errorText = findViewById(R.id.errorText);
        pinSection = findViewById(R.id.pinSection);
        qrSection = findViewById(R.id.qrSection);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Enter PIN"));
        tabLayout.addTab(tabLayout.newTab().setText("Scan QR"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    pinSection.setVisibility(View.VISIBLE);
                    qrSection.setVisibility(View.GONE);
                } else {
                    pinSection.setVisibility(View.GONE);
                    qrSection.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        pinInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                enterBtn.setVisibility(s.length() == 6 ? View.VISIBLE : View.GONE);
                errorText.setVisibility(View.GONE);
            }
        });

        enterBtn.setOnClickListener(v -> showNicknameDialog(pinInput.getText().toString().trim()));

        MaterialButton scanQrBtn = findViewById(R.id.scanQrBtn);
        scanQrBtn.setOnClickListener(v -> qrLauncher.launch(QRScannerHelper.createScanOptions()));
    }

    private void handleQRResult(ScanIntentResult result) {
        String contents = result.getContents();
        if (contents != null) {
            String pin = QRScannerHelper.extractPinFromUrl(contents);
            if (pin != null) {
                pinInput.setText(pin);
                showNicknameDialog(pin);
            } else {
                showError("Invalid QR code");
            }
        }
    }

    private void showNicknameDialog(String pin) {
        if (pin.length() != 6) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        EditText nickInput = new EditText(this);
        nickInput.setHint(R.string.nickname_hint);
        nickInput.setMaxLines(1);
        nickInput.setPadding(48, 32, 48, 32);

        builder.setTitle("Enter your nickname")
                .setView(nickInput)
                .setPositiveButton(R.string.lets_go, (dialog, which) -> {
                    String nickname = nickInput.getText().toString().trim();
                    if (!nickname.isEmpty()) {
                        joinGame(pin, nickname);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinGame(String pin, String nickname) {
        enterBtn.setEnabled(false);
        new Thread(() -> {
            try {
                String resp = ApiClient.get("/api/join?pin=" + pin + "&nickname=" + java.net.URLEncoder.encode(nickname, "UTF-8"));
                JSONObject json = new JSONObject(resp);

                if ("ok".equals(json.getString("status"))) {
                    int playerId = json.getInt("playerId");
                    int sessionId = json.getInt("sessionId");
                    String quizTitle = json.optString("quizTitle", "");

                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, LobbyActivity.class);
                        intent.putExtra("playerId", playerId);
                        intent.putExtra("sessionId", sessionId);
                        intent.putExtra("nickname", nickname);
                        intent.putExtra("quizTitle", quizTitle);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    String msg = json.optString("message", "Failed to join");
                    runOnUiThread(() -> showError(msg));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showError("Connection error: " + e.getMessage()));
            }
            runOnUiThread(() -> enterBtn.setEnabled(true));
        }).start();
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
