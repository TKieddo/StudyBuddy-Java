# Study Buddy AI

Course dashboard (subjects → documents → study tools).

- **Subjects** e.g. Java, Assembly Language (`+ Add subject`)
- **Docs** under each subject (PDF / image)
- Each doc: **summary, quiz, flashcards, 7-day plan**
- **Profile + scores** on the right, **Ask assistant** for the open doc
- Data saved in the `data/` folder

## 1. Add your Qwen Model Studio key

```bash
cd "StudyBuddy"
cp config.properties.example config.properties
```

Edit `config.properties`:

```properties
api.key=sk-sp-your-token-plan-key
api.baseUrl=https://token-plan.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1
api.model=qwen3.8-flash
api.visionModel=qwen3.8-flash
```

**Tested working models** on Token Plan: `qwen4-max`, `qwen3.8-max`, `qwen3.8-flash` (faster), `qwen3.7-max`, `qwen3.7-plus`.

Old names like `qwen-max` / `qwen-plus` → **404** on this endpoint.

No key? The app still opens, but **Import** and **Ask** show an error until you add a real key.

## 2. Compile and run

From the `StudyBuddy` folder:

```bash
javac -cp "lib/pdfbox-app.jar:." *.java
java -cp "lib/pdfbox-app.jar:." StudyBuddyApp
```

On Windows:

```bat
javac -cp "lib\pdfbox-app.jar;." *.java
java -cp "lib\pdfbox-app.jar;." StudyBuddyApp
```

## 3. Try it

1. Click **+ Add subject** (or open an existing one).
2. **Add PDF to scan** / **Add image to scan** with your key set.
3. Use Summary / Quiz / Flashcards / Schedule, then ask something in **Ask**.

## Lab report

Open `StudyBuddy_Report.html` in a browser → Print → Save as PDF (A4, print backgrounds on). Screenshots live in `report-screenshots/`.

## Notes

- Keep `config.properties` private (it is gitignored).
- Very long PDFs are cut to about 12,000 characters so requests stay smaller.
- Image import sends the picture to the AI (no separate OCR library).
