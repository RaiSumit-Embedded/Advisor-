# LifePilot — v0.2 (Health + Finance + AI Advisor)

3 tabs:
- HOME   : steps (phone sensor) + neend (manual log), 7-din history
- PAISA   : bank/UPI transaction SMS auto-read -> is mahine kharch/aaya + list
- ADVISOR : tera data AI ko dekar aaj ka sharp coaching (Claude API)

Naya logo + fresh UI (gradient header, bottom tabs).

## APK banao (GitHub Actions, no Android Studio)
1. Naye/changed files repo me upload karo (neeche "update kaise kare" dekho).
2. Commit -> "Actions" tab -> build (~4 min) -> green tick.
3. Run kholo -> Artifacts -> LifePilot-debug-apk -> download -> app-debug.apk install.

## Permissions (pehli baar app me)
- Physical Activity  -> steps ke liye (Home tab).
- SMS               -> Paisa tab me "SMS access do" dabao. Sirf transaction SMS
                       parse hote hain, data phone me hi rehta hai, kahin nahi jaata.
- Internet          -> Advisor ke API call ke liye (auto).

## AI Advisor setup (FREE - Google Gemini)
1. aistudio.google.com pe Google account se login (koi card/paise nahi).
2. "Get API key" / "Create API key" -> key AIza... se shuru hogi. Copy kar.
3. App -> Advisor tab -> Settings -> key paste -> Save.
4. Model default: gemini-2.5-flash (free). "Aaj ka insight lo" dabao.
Free tier me thodi rate limit hai (~din me kaafi calls), personal use ke liye theek.
Key phone me hi save; sirf Google ke API call me jaati hai.

Package: com.spectra.lifepilot
