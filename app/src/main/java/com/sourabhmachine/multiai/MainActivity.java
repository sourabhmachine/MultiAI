package com.sourabhmachine.multiai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText prompt, apiKey;
    private TextView response;
    private Spinner provider;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        provider = findViewById(R.id.providerSpinner);
        apiKey = findViewById(R.id.apiKey);
        prompt = findViewById(R.id.prompt);
        response = findViewById(R.id.response);
        String[] providers = {"Ollama", "Gemini", "Groq", "OpenRouter"};
        provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers));
        findViewById(R.id.sendButton).setOnClickListener(v -> send());
    }

    private void send() {
        String p = prompt.getText().toString().trim();
        if (p.isEmpty()) { response.setText("Enter a prompt first."); return; }
        String selected = provider.getSelectedItem().toString();
        response.setText("Selected: " + selected + "\n\nConnecting…");
        executor.execute(() -> runProvider(selected, p));
    }

    private void runProvider(String selected, String promptText) {
        // Provider transport is intentionally isolated here so API implementations can be added safely.
        runOnUiThread(() -> response.setText("Selected: " + selected + "\n\nProvider connection is ready for configuration.\n\nPrompt:\n" + promptText));
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
