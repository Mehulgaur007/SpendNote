# SpendNote – Offline Expense Tracker (Android)

A fully offline daily expense & income tracker. No internet, no login, no ads.
Built with Kotlin, Jetpack Compose (Material 3) and Room (local SQLite database).

## Features
- **Running balance** – set your starting balance (e.g. ₹3000); every expense subtracts, every income adds.
- **Quick entry** – type "Tea 20", "Petrol 300", "Salary 15000" and tap Fill. Category and type are guessed offline.
- **Daily ledger** – entries grouped by day with daily spend totals, month by month.
- **Insights** – week / month / year view, spending trend bar chart, category donut chart with percentages, average per day.
- **Budgets** – overall monthly budget with "money left" and daily allowance, plus per-category limits and past months history.
- **Backup** – export all data to CSV (opens in Excel/Sheets) and import it back on a new phone.
- Dark mode, Indian number grouping (1,00,000), selectable currency symbol.

## Option A – Build with Android Studio
1. Install Android Studio (Koala 2024.1 or newer).
2. Unzip this folder and choose **File ▸ Open** → select the `SpendNote` folder.
3. Wait for Gradle sync to finish (first time downloads ~500 MB of libraries).
   - If Android Studio says the Gradle wrapper is missing, accept its offer to create/use it (Gradle 8.7).
4. Connect your phone (USB debugging on) and press ▶ Run, **or**
   **Build ▸ Build App Bundle(s)/APK(s) ▸ Build APK(s)** → APK is in `app/build/outputs/apk/debug/`.

## Option B – Get the APK from GitHub (no Android Studio needed)
1. Create a new GitHub repository and upload all files in this folder (keep the `.github` folder).
2. GitHub Actions builds automatically. Open the **Actions** tab → latest "Build APK" run.
3. Download the **SpendNote-apk** artifact (zip) → install the APK on your phone
   (allow "Install unknown apps" for your file manager/browser).

## Customising
- App name: `app/src/main/res/values/strings.xml`
- Colors: `app/src/main/java/com/spendnote/app/ui/theme/Theme.kt`
- Categories & emojis: `data/Models.kt`
- Quick-entry keywords: `data/QuickParse.kt`

## Project structure
```
app/src/main/java/com/spendnote/app/
  MainActivity.kt        – entry point
  MainViewModel.kt       – state, settings, CSV backup/restore
  data/                  – Room database, entities, categories, quick-entry parser
  util/Format.kt         – money/date formatting, totals helpers
  ui/                    – Home, Add/Edit, Insights, Budget, Settings screens + charts
```

Note: the debug APK is signed with a debug key – fine for personal use. To publish on Play Store,
create a release keystore (Build ▸ Generate Signed Bundle/APK).
