# Qognix DocuFlow — Native App v3.0 (APK banane ke steps)

Ye **proper native app** hai. Is mein website load NAHI hoti, koi WebView NAHI hai.
Saare features app ke andar bane hain: PDF upload, Summary, MCQs, Quiz, Ask the PDF,
Translate, AI Chat, Gemini key box.

Package: com.qognix.docuflow — Version 3.0 (purani v1 wali app ki jagah install hogi)

## APK free mein banane ka tareeqa (GitHub, phone se)

1. Apna wahi purana GitHub repo kholo: abdulrehman288737-bot/qognix-docuflow-app
2. Us repo ki purani files delete karke is ZIP ke andar wale `project` folder ki
   SAARI files wahan upload karo (ya naya repo bana kar upload karo).
   NOTE: keystore wali file kabhi upload NAHI karni.
3. Repo mein **Actions** tab kholo → "Build Qognix DocuFlow APK" → **Run workflow**.
4. Build green (✓) ho jaye to neeche **Artifacts** mein `QognixDocuFlow-apks` milega.
   Us ZIP ko download karo, us ke andar `app-debug.apk` hai — wahi install karni hai.
5. Phone par install karke test karo:
   - PDF upload karo → pages aur text read hota hai ya nahi
   - Summary / MCQ / Quiz banao
   - Gemini key paste karke "Save, Test & Use Key" dabao → "Gemini ON" aana chahiye
   - Ask the PDF aur Chat try karo
6. Agar build red (✗) ho jaye to Actions wale page ki error wali screenshot / text
   mujhe bhej dena — main fix karke naya ZIP dunga.

## Sach wali baatein
- Local computer par APK build network permission ki wajah se nahi ho sakta,
  is liye GitHub wala free rasta hi proven rasta hai (v1 isi se bani thi).
- Compilation abhi GitHub build se hi verify hogi — main jhoot nahi bolunga ke
  APK tayyar hai jab tak GitHub build green na ho.
- Ads: v3.0 mein Start.io ke chaaron format hain (user ki apni Start.io App ID
  210654162) — neeche banner, Documents mein ek labelled native ad, result
  screen se wapas ane par interstitial (time cap ke sath), aur home par
  rewarded video wala button. Debug (test) APK mein test ads ate hain,
  release mein asal ads. Koi popunder ya fake ad nahi hai.
