# SplitSnap

Snap the bill, tap who had what, and everyone pays their share. SplitSnap is a native Android bill splitter built with Kotlin, Jetpack Compose, CameraX, on-device ML Kit OCR, and Room.

GitHub: https://github.com/george-killua/receipt-scanner-compose

## How it works

1. **Scan, import, or type a bill.** Take a photo of a receipt or import an image or PDF. SplitSnap reads the line items, tax, tip, and total on the phone; nothing is uploaded.
2. **Add the people at the table.** Names you've used before appear as one-tap suggestions.
3. **Pick a person, then tap what they had.** Tap a dish for several people to share it. "Share the rest with everyone" handles the drinks and the bread.
4. **See the split.** Shares are exact to the cent: shared items are divided evenly, and tax and tip follow each person's subtotal. A check compares the bill with the receipt total, so you notice a misread line.
5. **Send everyone their share** through any messaging app, then tick people off as they pay you back.

## Architecture

- `domain`: pure Kotlin. `ReceiptRows` rebuilds receipt rows from OCR bounding boxes, `ReceiptParser` pulls out line items, tax, tip, and total, `SplitCalculator` does the cent-exact split, and `Money` handles parsing and formatting.
- `data`: Room tables for bills, people, items, and item assignments, plus `BillRepository`
- `ui`: Compose screens (home, camera, bill editor, summary) driven by a single `SplitViewModel`

## Run

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Or open the project in Android Studio and run the `app` configuration.

## Notes

The receipt parser uses rules and is tuned for common English and German receipt layouts. It handles VAT that is already included in prices, quantity prefixes such as `2 x`, and discount lines. Unusual layouts may need manual edits in the bill editor.
