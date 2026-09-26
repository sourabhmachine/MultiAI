package com.sourabhmachine.multiai;

import android.app.Activity;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "MultiAIKey";
    private static final String PREFS = "multiai_secure";
    private static final String PREFIX = "key_";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Spinner provider; private EditText model, apiKey, prompt; private TextView status, response; private CheckBox autoMode;
    private final String[] providers = {"Auto", "Gemini", "Groq", "OpenRouter", "Cerebras", "Mistral", "Together AI", "SambaNova"};
    private final Map<String,String> defaults = new HashMap<>();
    private final Map<String,String> endpoints = new HashMap<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        provider=findViewById(R.id.providerSpinner); model=findViewById(R.id.model); apiKey=findViewById(R.id.apiKey); prompt=findViewById(R.id.prompt); status=findViewById(R.id.status); response=findViewById(R.id.response); autoMode=findViewById(R.id.autoMode);
        provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers));
        defaults.put("Gemini","gemini-2.5-flash"); defaults.put("Groq","openai/gpt-oss-120b"); defaults.put("OpenRouter","openrouter/free"); defaults.put("Cerebras","llama-3.3-70b"); defaults.put("Mistral","mistral-small-latest"); defaults.put("Together AI","meta-llama/Llama-3.3-70B-Instruct-Turbo"); defaults.put("SambaNova","Meta-Llama-3.1-70B-Instruct");
        endpoints.put("Groq","https://api.groq.com/openai/v1/chat/completions"); endpoints.put("OpenRouter","https://openrouter.ai/api/v1/chat/completions"); endpoints.put("Cerebras","https://api.cerebras.ai/v1/chat/completions"); endpoints.put("Mistral","https://api.mistral.ai/v1/chat/completions"); endpoints.put("Together AI","https://api.together.xyz/v1/chat/completions"); endpoints.put("SambaNova","https://api.sambanova.ai/v1/chat/completions");
        provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p, android.view.View v,int pos,long id){String x=providers[pos]; if(x.equals("Auto")){model.setText("");apiKey.setText("");return;} model.setText(defaults.get(x)); loadKey(x);} public void onNothingSelected(AdapterView<?> p){}});
        findViewById(R.id.saveKey).setOnClickListener(v -> saveKey()); findViewById(R.id.sendButton).setOnClickListener(v -> send());
        autoMode.setOnCheckedChangeListener((button, checked) -> { if(checked) provider.setSelection(0); });
    }

    private String prefsName(String p){return PREFIX+p.replace(" ","_");}
    private void loadKey(String p){try{apiKey.setText(readSecret(p));}catch(Exception e){apiKey.setText("");status.setText("Secure key storage unavailable: "+e.getMessage());}}
    private void saveKey(){String p=provider.getSelectedItem().toString(); if(p.equals("Auto")){status.setText("Select a provider to save its key.");return;} try{writeSecret(p,apiKey.getText().toString().trim());status.setText(p+" key saved securely on this device.");}catch(Exception e){status.setText("Could not save key securely: "+e.getMessage());}}

    private void send(){String q=prompt.getText().toString().trim(); if(q.isEmpty()){status.setText("Enter a prompt first.");return;} response.setText(""); status.setText("Sending…"); executor.execute(() -> {try{String p=provider.getSelectedItem().toString(); String out=(p.equals("Auto")||autoMode.isChecked())?auto(q):call(p,q,apiKey.getText().toString().trim(),model.getText().toString().trim()); runOnUiThread(()->{status.setText("Completed");response.setText(out);});}catch(Exception e){runOnUiThread(()->{status.setText("Request failed");response.setText(e.getMessage());});}});}
    private String auto(String q)throws Exception{String[] order={"Gemini","Groq","OpenRouter","Cerebras","Mistral","Together AI","SambaNova"}; List<String> errors=new ArrayList<>(); for(String p:order){String k=readSecret(p); if(k.isEmpty())continue; try{return call(p,q,k,defaults.get(p));}catch(Exception e){errors.add(p+": "+e.getMessage());}} throw new Exception(errors.isEmpty()?"No provider API keys are configured.":"All configured providers failed:\n"+String.join("\n",errors));}
    private String call(String p,String q,String key,String mdl)throws Exception{if(key.isEmpty())throw new Exception("API key missing for "+p); if(mdl.isEmpty())mdl=defaults.get(p); if(p.equals("Gemini"))return gemini(q,key,mdl); return openAiCompatible(p,q,key,mdl);}
    private String gemini(String q,String key,String mdl)throws Exception{String url="https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(mdl,"UTF-8")+":generateContent?key="+URLEncoder.encode(key,"UTF-8"); String body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",q))))).toString(); String r=http(url,null,body); JSONObject o=new JSONObject(r); JSONArray c=o.optJSONArray("candidates"); if(c!=null&&c.length()>0)return c.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text",r); return r;}
    private String openAiCompatible(String p,String q,String key,String mdl)throws Exception{String body=new JSONObject().put("model",mdl).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",q))).put("temperature",0.7).toString(); String r=http(endpoints.get(p),key,body); JSONObject o=new JSONObject(r); JSONArray choices=o.optJSONArray("choices"); if(choices!=null&&choices.length()>0)return choices.getJSONObject(0).getJSONObject("message").optString("content",r); return r;}
    private String http(String url,String key,String body)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(60000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json"); if(key!=null)c.setRequestProperty("Authorization","Bearer "+key); byte[] b=body.getBytes(StandardCharsets.UTF_8); try(OutputStream os=c.getOutputStream()){os.write(b);} int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String r=read(in); c.disconnect(); if(code<200||code>=300)throw new Exception("HTTP "+code+": "+r); return r;}
    private String read(InputStream in)throws Exception{if(in==null)return ""; BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); StringBuilder s=new StringBuilder(); String x; while((x=br.readLine())!=null)s.append(x); return s.toString();}

    private SecretKey getKey() throws Exception {
        KeyStore ks=KeyStore.getInstance(KEYSTORE); ks.load(null); if(!ks.containsAlias(KEY_ALIAS)){KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,KEYSTORE); kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()); kg.generateKey();} return ((KeyStore)ks).getKey(KEY_ALIAS,null) instanceof SecretKey ? (SecretKey)ks.getKey(KEY_ALIAS,null) : null;
    }
    private void writeSecret(String provider,String value)throws Exception{if(value.isEmpty()){getPreferences(0).edit().remove(prefsName(provider)).apply();return;} SecretKey key=getKey(); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key); byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)); String packed=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP); getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(prefsName(provider),packed).apply();}
    private String readSecret(String provider)throws Exception{String packed=getSharedPreferences(PREFS,MODE_PRIVATE).getString(prefsName(provider),""); if(packed.isEmpty())return ""; String[] parts=packed.split(":",2); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,getKey(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP))); return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
