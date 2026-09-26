package com.sourabhmachine.multiai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
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
        provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p, android.view.View v,int pos,long id){String x=providers[pos]; if(!x.equals("Auto")){model.setText(defaults.get(x)); loadKey(x);} } public void onNothingSelected(AdapterView<?> p){}});
        findViewById(R.id.saveKey).setOnClickListener(v -> saveKey()); findViewById(R.id.sendButton).setOnClickListener(v -> send());
    }
    private String prefsName(String p){return "key_"+p.replace(" ","_");}
    private void loadKey(String p){apiKey.setText(getPreferences(0).getString(prefsName(p),""));}
    private void saveKey(){String p=provider.getSelectedItem().toString(); if(p.equals("Auto")){status.setText("Select a provider to save its key.");return;} getPreferences(0).edit().putString(prefsName(p),apiKey.getText().toString().trim()).apply(); status.setText(p+" key saved on this device.");}
    private void send(){String q=prompt.getText().toString().trim(); if(q.isEmpty()){status.setText("Enter a prompt first.");return;} response.setText(""); status.setText("Sending…"); executor.execute(() -> {try{String p=provider.getSelectedItem().toString(); String out=p.equals("Auto")?auto(q):call(p,q,apiKey.getText().toString().trim(),model.getText().toString().trim()); runOnUiThread(()->{status.setText("Completed");response.setText(out);});}catch(Exception e){runOnUiThread(()->{status.setText("Request failed");response.setText(e.getMessage());});}});}
    private String auto(String q)throws Exception{String[] order={"Gemini","Groq","OpenRouter","Mistral","Cerebras","Together AI","SambaNova"}; List<String> errors=new ArrayList<>(); for(String p:order){String k=getPreferences(0).getString(prefsName(p),""); if(k.isEmpty())continue; try{return call(p,q,k,defaults.get(p));}catch(Exception e){errors.add(p+": "+e.getMessage());}} throw new Exception(errors.isEmpty()?"No provider API keys are configured.":"All configured providers failed: "+errors);}
    private String call(String p,String q,String key,String mdl)throws Exception{if(key.isEmpty())throw new Exception("API key missing for "+p); if(mdl.isEmpty())mdl=defaults.get(p); if(p.equals("Gemini"))return gemini(q,key,mdl); return openAiCompatible(p,q,key,mdl);}
    private String gemini(String q,String key,String mdl)throws Exception{String url="https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(mdl,"UTF-8")+":generateContent?key="+URLEncoder.encode(key,"UTF-8"); String body="{\"contents\":[{\"parts\":[{\"text\":"+json(q)+"}]}]}"; String r=http(url,null,body); String t=extract(r,"\"text\":\""); return t.isEmpty()?r:t;}
    private String openAiCompatible(String p,String q,String key,String mdl)throws Exception{String body="{\"model\":"+json(mdl)+",\"messages\":[{\"role\":\"user\",\"content\":"+json(q)+"}],\"temperature\":0.7}"; String r=http(endpoints.get(p),key,body); String t=extract(r,"\"content\":\""); return t.isEmpty()?r:t;}
    private String http(String url,String key,String body)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(60000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json"); if(key!=null)c.setRequestProperty("Authorization","Bearer "+key); byte[] b=body.getBytes(StandardCharsets.UTF_8); c.getOutputStream().write(b); int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String r=read(in); if(code<200||code>=300)throw new Exception("HTTP "+code+": "+r); return r;}
    private String read(InputStream in)throws Exception{if(in==null)return ""; BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); StringBuilder s=new StringBuilder(); String x; while((x=br.readLine())!=null)s.append(x); return s.toString();}
    private String json(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
    private String extract(String s,String marker){int i=s.indexOf(marker); if(i<0)return ""; i+=marker.length(); StringBuilder o=new StringBuilder(); boolean esc=false; for(int j=i;j<s.length();j++){char c=s.charAt(j); if(esc){o.append(c=='n'?'\n':c=='r'?'\r':c);esc=false;} else if(c=='\\')esc=true; else if(c=='\"')break; else o.append(c);} return o.toString();}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
