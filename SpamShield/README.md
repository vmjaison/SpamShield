# SpamShield — offline Android call blocker

Free Android starter app. Android 10+ (API 29+). Uses the system CallScreeningService role and an app-owned SQLite blocklist.

## Build
1. Install Android Studio and Android SDK Platform 35 (and JDK 17).
2. Open this directory as a project; allow Gradle sync and SDK downloads.
3. Build > Build APK(s) or run on a physical Android 10+ device.
4. Open SpamShield and tap **Enable call screening**; approve the Android role prompt.
5. Add a test phone number (prefer full international format, e.g. +91...). Call from that number to test. Do not test using emergency numbers.

## Important limitations
- This is a working **local blocklist-based screening MVP**, not a comprehensive spam identification product. It can download a third-party spam-number database on demand (not bundled). It does not include AI or real-time caller lookup.
- Numbers matching the downloaded reputation database will be rejected after a successful manual sync. Unknown numbers not in the feed are allowed.
- Numbers are stored in the app's SQLite database, **not** Android's system-wide blocked-number provider.
- Screening requires the user to assign the app the call-screening role. Android/OEM behavior and which calls are screened can vary; hidden/withheld numbers cannot be matched.
- Android invokes screening with strict response timing. Avoid network requests in onScreenCall.
- Phone number normalization here is basic; for production use a country-aware library such as libphonenumber, and handle national/international equivalence.
- This source has not been compiled or tested in this environment; verify on a real phone before relying on it.
- For a production release, add accessibility, backup/export, database import validation, unit/instrumentation tests and explicit user consent for optional reputation services.

## Privacy
The app has INTERNET permission to download the public feed on user request. It does not send caller numbers to the feed server. No paid APIs.

## Build online without Android Studio (GitHub Actions)

1. Create a new **private** repository on GitHub (free).
2. Upload the **contents of the `SpamShield` folder** to the repository root, including the hidden `.github/workflows/build-apk.yml` file. Do not upload the ZIP as a single file.
3. Open **Actions** > **Build SpamShield APK** > **Run workflow** (or push to `main`).
4. After the workflow completes, open its run and download the **SpamShield-debug-apk** artifact.
5. Unzip that artifact to obtain `app-debug.apk`; transfer it to your Android phone and install it. You may need to allow installs from your browser/file manager.
6. Open SpamShield and grant the **Call screening** role. Add a test number to the blocklist.

This is a **debug APK** for personal testing, not a Play Store release. After a successful feed download, it also rejects numbers matching the downloaded community spam database. The APK build has not been independently verified in this environment. GitHub's free Actions usage and Android compatibility are subject to GitHub/Android limitations.

## Community reputation feed (new)

Tap **Download / update spam database** in the app with internet access. This retrieves the existing CallShield public legacy dataset at https://github.com/SysAdminDoc/CallShield (MIT-licensed project). The app imports exact phone numbers into SQLite and checks the database **offline during incoming calls**. It never performs a network request while screening. The importer rejects malformed/empty datasets and preserves the previous list on failure. It does not verify the feed's cryptographic signatures; this is a testing MVP, not production-grade feed security.

**Important:** The upstream feed may change its format or URL. The importer recognizes arrays of number strings, arrays of objects with `number`/`phone` fields, or number-keyed JSON objects; it reports failure otherwise. The current feed could not be downloaded and tested in this build environment. CallShield's data is largely North American, so Indian spam-number coverage may be limited. False positives are possible; add trusted numbers to the allowlist. Manual blocks override trusted numbers. Avoid using for critical calls until tested.

To update your existing GitHub repository, replace the old project files with the contents of this ZIP, commit to `main`, and run the Actions workflow.

## India community scam reports (new)
The app can download `spam-calls.csv` from https://github.com/makash/scam-calls-india (Unlicense). It imports **only** records with `country_code` +91 and a valid 10-digit Indian mobile number. The list is very small (only one valid Indian mobile number when checked on 2026-10-12), and its reports are not verified. The `reported` column does **not** imply independently verified spam. For this reason **automatic rejection from this Indian list is OFF by default**; the user must explicitly enable it. Personal manual blocks always apply. The source is downloaded on demand, not bundled. There is no claim of nationwide coverage.

To update: open the app, tap **Download / update Indian list**, then optionally enable the checkbox after reviewing the risks. Previously downloaded records are retained if an update fails. The separate international list remains available. This is a source project, not a compiled or tested APK.

## Additional Indian spam lists (v1.1)
Choose **Import Indian CSV / TXT** to import another legally obtained dataset. Supported: one Indian mobile number per line, or a CSV with a `number`, `phone_number`, `phone`, or `mobile` column. Accepts 10-digit Indian mobile numbers and +91/91 prefixes. Invalid entries are ignored; imports are limited to 5 MB and 150,000 lines. The import replaces the previous *user-imported* list only, preserving manual blocks, the CallShield list and the separate makash list. It is not a claim to contain every Indian spam number.

The imported list's automatic rejection toggle is **OFF by default** because the source may be inaccurate. You must enable it explicitly after reviewing the file. Trusted numbers override downloaded/imported matches; manually blocked numbers remain blocked. The app does not upload the imported list. For better coverage, only use phone-number datasets with a compatible license and documented provenance; SMS message datasets are not phone-number blocklists.

ScamShield India (`V3RNIT/scamshield-india`) documents a lookup API but not a verified public bulk export, so it is not silently downloaded into the APK. Commercial/private lists are not included. This project has not been built or device-tested here.
