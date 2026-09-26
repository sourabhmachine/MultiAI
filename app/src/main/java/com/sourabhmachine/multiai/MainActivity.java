package com.sourabhmachine.multiai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText prompt;
    private TextView response;
    private Spinner provider;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        provider = findViewById(R.id.providerSpinner);
        prompt = findViewById(R.id.prompt);
        response = findViewById(R.id.response);
        String[] providers = {"Gemini", "Groq", "OpenRouter"};
        provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers));
        findViewById(R.id.sendButton).setOnClickListener(v -> send());
    }

    private void send() {
        String p = prompt.getText().toString().trim();
        if (p.isEmpty()) { response.setText("Enter a prompt first."); return; }
        String selected = provider.getSelectedItem().toString();
        response.setText("Selected: " + selected + "\n\nProvider connection is being configured…");
        executor.execute(() -> runOnUiThread(() -> response.setText("Selected: " + selected + "\n\nReady for API configuration.\n\nPrompt:\n" + p)));
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
