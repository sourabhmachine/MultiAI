# MultiAI Secure — Android

Cloud-only multi-model Android chat app for a Samsung S24+. No PC/local model is required.

## Providers
- **FreeModel** — primary automatic router using `https://freemodel.online/v1`; `auto/best-free` is the default route. Its current documentation says it routes across a large model pool, tries free capacity first, and can fail over when a provider is unavailable.
- **Gemini** — direct Google API.
- **Groq** — direct OpenAI-compatible API.
- **OpenRouter** — direct API, with `openrouter/free` available where supported.
- **FreeModels.pro** — included as a one-tap web option. Its public interface does not expose a documented official mobile API, so the app does not pretend it is a native API provider.

## Auto mode
Default order: **FreeModel → Gemini → Groq → OpenRouter**. If a configured provider fails, the app tries the next configured provider. It never attempts to bypass authentication, CAPTCHAs, quotas, or rate limits.

## Security
- API keys are encrypted at rest using Android Keystore-backed storage.
- Android app backup is disabled.
- No hard-coded API keys.
- HTTPS is used for cloud API calls.
- No app-owned backend.
- No client-only app can honestly be called hack-proof; a rooted or compromised phone can expose secrets while they are in use.

## Build without Android Studio
The repository includes `.github/workflows/build-apk.yml`. Push this project to a GitHub repository, then run **Actions → Build APK**. GitHub Actions builds the debug APK and publishes it as an artifact named `MultiAI-debug-apk`.

## Configure
Open **Settings** in the app and add the API keys you personally obtain from the providers. Never send API keys to anyone else.

## Important distinction
There are several similarly named services online. This app uses **FreeModel.online** as the API gateway because it has documented OpenAI-compatible endpoints. **FreeModels.pro** is retained as a web option because its public web interface is not a documented official API.
