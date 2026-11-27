# GenDev AI — Gemini Pro Research & Android Build Studio

**GenDev AI** adalah aplikasi Android native (Kotlin, Jetpack Compose, Material 3) yang difokuskan secara mendalam pada ekosistem model **Google Gemini** (Pro, Flash, Flash-Lite) untuk riset, eksperimen, coding Android, simulasi, serta sinkronisasi & build APK otomatis ke **GitHub Releases** tanpa PC.

## Fitur Utama
- **Eksklusif Google Gemini Suite**:
  - `Gemini 3.8 Flash` (default multimodal latensi rendah)
  - `Gemini 4 Argon (Pro)` (flagship reasoning, arsitektur & coding kompleks)
  - `Gemini 3.5 Flash-Lite` (ultra-cepat untuk eksperimen & ACE)
- **Dual Execution Engine**:
  - **Firebase AI Logic (Built-in)** — langsung berjalan aman via Firebase App Check tanpa perlu kunci API manual.
  - **BYOK Gemini API / Secure HTTPS Proxy** — mendukung Gemini API Key berbayar milik pengguna atau endpoint backend proxy HTTPS, terenkripsi dengan **Android Keystore (AES-256-GCM)**.
- **Tanpa Batas Buatan Aplikasi**: Tidak ada sistem kredit, tidak ada paywall internal, tidak ada batas pesan harian, dan tidak ada system prompt pembatas tersembunyi.
- **ACE (Adaptive Conversation Engine)**:
  - Deteksi & klasifikasi transparan error API, rate limit (429), timeout, context overflow, dan kebijakan keselamatan penyedia.
  - **Smart Rephrasing** dengan tampilan prompt asli vs perbaikan serta opsi **Auto Rephrase** & **persetujuan manual**.
  - **Context Compression** otomatis maupun manual saat mendekati batas context window.
  - **Automatic Recovery** untuk gangguan jaringan sementara.
- **GitHub Android Build Studio (Push to GitHub & Auto-Build APK)**:
  - Generate proyek Android Jetpack Compose lengkap dengan Gemini.
  - Editor file & inspeksi `.github/workflows/android-apk-release.yml` langsung di aplikasi.
  - Tombol **Push to GitHub & Build APK Release** (GitHub REST API v3 atomic commit) yang otomatis membuat/memperbarui repo, menjalankan GitHub Actions, dan menerbitkan **Debug APK + Release APK** ke menu **GitHub Releases**.
  - Pantau status GitHub Actions dan unduh APK hasil build langsung dari aplikasi.
- **Usage & Cost Dashboard**: Pelacakan token input/output, estimasi biaya per model Gemini, latensi, dan log lokal di **Room Database**.

## Automated Build & GitHub Releases (Tanpa PC)
Repository ini dilengkapi `.github/workflows/android-build-release.yml` yang otomatis berjalan pada setiap `push` ke `main`/`master` atau `workflow_dispatch`:
1. Menyiapkan JDK 17 dan Gradle otomatis.
2. Membangun `GenDevAI-debug.apk` dan `GenDevAI-release.apk` (mendukung GitHub Secrets `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, dengan fallback aman jika secret belum diatur).
3. Mengunggah APK ke Artifacts dan mempublikasikannya otomatis di tab **Releases**.
