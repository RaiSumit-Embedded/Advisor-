# LifePilot — Slice 1 (no watch needed)

Steps = phone ka built-in step-counter sensor (jeb me phone -> steps khud ginega).
Neend = manually log (slider se ghante). Sab data phone me hi save. 7-din history.

## APK banao — OPTION A: GitHub Actions (no Android Studio) ⭐
1. github.com pe free account + "New repository".
2. Is folder ke SAARE files/folders upload karo (.github folder bhi zaroor).
3. "Actions" tab pe build khud chalega (~3-5 min).
4. Green tick -> run kholo -> "Artifacts" -> "LifePilot-debug-apk" download -> andar app-debug.apk.
5. Phone pe install (Settings me "unknown apps" allow).

## OPTION B: Android Studio
File > Open > yeh folder -> Run, ya Build > Build APK(s).

## Pehli baar app me
- App khulte hi "Physical Activity" permission maangega -> Allow (iske bina steps 0).
- Thoda chalo, phone jeb me rakho -> "Aaj ke steps" badhega.
- Neche "Neend log" FAB -> slider se ghante set -> Save.

Note: kuch phones (aur emulator) me step sensor nahi hota -> app batayega, steps 0 rahenge.
Package: com.spectra.lifepilot
