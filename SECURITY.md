# Security Model

## Protected
- Secrets are not committed to source.
- Secrets are stored using `EncryptedSharedPreferences` with an Android Keystore-backed AES-256-GCM master key.
- Android backup/data transfer excludes the credential store.
- Cloud requests use HTTPS.
- Gemini key is sent in the `x-goog-api-key` header rather than a URL query string.
- No analytics SDK or remote app backend is included.

## Limitations
- A rooted or instrumented device can potentially access secrets after decryption/use.
- The provider sees the prompt and account metadata according to its own policy.
- OpenRouter/Groq/Gemini free limits are controlled by those providers.
- A local Ollama endpoint must be secured by the user; this app does not add authentication to Ollama.

## Recommended hardening
- Use provider-side key restrictions/quotas where offered.
- Create a separate key for this app rather than using a key shared with other projects.
- Rotate keys periodically and immediately after suspected exposure.
- Keep Android, Google Play system updates and the app updated.
- Use a screen lock and avoid debugging/USB authorization on untrusted computers.
- For high-security deployments, put provider credentials behind your own authenticated backend rather than storing them on-device.
