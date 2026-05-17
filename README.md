# Receipt Scanner Compose

Native Android receipt scanning sample built with Kotlin and Jetpack Compose.

## Features

- CameraX receipt capture
- Image and PDF import from device storage
- ML Kit OCR text recognition
- Field extraction for merchant, date, and total amount
- Review/edit screen before saving
- Local original-file storage
- Room database persistence
- Compose receipt list and detail view

## Run

```bash
./gradlew :app:assembleDebug
```

Open the project in Android Studio and run the `app` configuration on a device or emulator.

## Notes

PDF OCR renders the first page before text recognition. Real production use should add multi-page PDF handling and receipt-specific parser tuning from sample receipts.
