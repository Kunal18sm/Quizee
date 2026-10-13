# 📱 Quiz App Locker (Android) - 2,580+ Static GK Questions Edition

Android Mobile Application jo selected apps par ek interactive **Quiz Lock** lagati hai. Jab bhi user kisi locked app (jaise Instagram, WhatsApp, Games, etc.) ko open karega, to pehle **5 MCQs** ka answer karna hoga.

---

## ✨ Features & Functionality

1. **5 MCQ Quiz Lock Screen**:
   - Selected app open karte hi Quiz Screen overlay display hoti hai.
   - Database me moujood **2,584 Static GK questions** me se randomly 5 MCQs aate hain.
   - Exit to Home Screen button diya gaya hai taaki bina quiz complete kiye locked app open na ho sake.
2. **Instant Color Feedback**:
   - Option par click karte hi agar answer **Correct** hai to option **Green (`#10B981`)** ho jata hai with ✓ check icon.
   - Agar wrong option select kiya to clicked option **Red (`#EF4444`)** ho jata hai with ✗ cross icon, aur **Correct answer automatically Green** ho jata hai.
3. **App Selection Dashboard**:
   - Phone me installed sabhi apps ki list real icons aur names ke saath dikhayi deti hai.
   - Live Search bar se kisi bhi app ko turant dhund kar Switch on/off karke lock kar sakte hain.
4. **Comprehensive Static GK Bank (2,584 MCQs Loaded)**:
   - 128-page Static GK 2026 PDF ka ek-ek topic aur row cover kiya gaya hai:
     - 485 Sports Terminology MCQs
     - 444 Wildlife Sanctuaries & National Parks (All States) MCQs
     - 255 Dams in India (River & State) MCQs
     - 231 Famous Books and Authors MCQs
     - 209 Dances of India (Classical & Folk State-wise) MCQs
     - 83 Stadiums in India MCQs
     - 74 Military Exercises (Domestic, Bilateral, Multilateral) MCQs
     - 65 Historic Forts of India MCQs
     - 63 First in World & India (Male & Milestones) MCQs
     - 62 Famous Personalities & Nicknames MCQs
     - 57 Famous Museums of India MCQs
     - 55 Important Institutions in India MCQs
     - 50 Countries, Capitals, Currencies & Parliaments MCQs
     - 49 Important Full Forms MCQs
     - 45 Superlatives of India MCQs
     - 42 First in World & India (Female) MCQs
     - 38 Inventions and Inventors MCQs
     - 31 Rivers of India MCQs
     - 30 Mountain Peaks in India MCQs
     - 28 Thermal Power Plants MCQs
     - 21 Waterfalls in India MCQs
     - 21 Awards and Honours MCQs
     - 18 World Heritage Sites MCQs
     - 16 Famous Temples in India MCQs
     - 16 International Airports in India MCQs
     - 16 International Boundary Lines MCQs
     - 14 Space Centres & ISRO Agencies MCQs
     - 12 Sports Personalities Nicknames MCQs
     - 11 Major Sea Ports of India MCQs
     - 9 Major Crops and Producing States MCQs
     - 9 Famous Gardens in India MCQs
     - 6 Biosphere Reserves MCQs
     - 6 Nobel Prize Winners 2025 MCQs
5. **Custom Quiz Management (In-App)**:
   - App me upar **"Questions"** button par tap karke phone se hi direct naye questions add ya delete kar sakte hain.
6. **Test Quiz Mode**:
   - Dashboard me **"Test Quiz"** button diya gaya hai jisse aap bina kisi app ko lock kiye turant quiz screen ka visual feedback check kar sakte hain.

---

## 📂 Project Structure

```
quizeApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   └── questions.json            <-- 2,584 Complete Static GK MCQs (745 KB)
│   │   ├── java/com/quizlock/app/
│   │   │   ├── MainActivity.kt           <-- Apps list & Permissions dashboard
│   │   │   ├── data/
│   │   │   │   ├── Question.kt           <-- MCQ data model
│   │   │   │   ├── QuestionRepository.kt  <-- Question loader & manager
│   │   │   │   └── PreferencesManager.kt  <-- Lock settings & sessions
│   │   │   ├── service/
│   │   │   │   ├── AppLockAccessibilityService.kt <-- Instant app open detector
│   │   │   │   └── AppLockForegroundService.kt    <-- Fallback background monitor
│   │   │   └── ui/
│   │   │       ├── QuizLockActivity.kt    <-- 5 MCQ Lock Screen (Green/Red feedback)
│   │   │       └── QuestionManageActivity.kt <-- Questions Editor screen
│   │   └── res/                          <-- Layouts, Colors, Drawables
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 How to Run the App

1. **Android Studio** open karein.
2. **Open** par click karein aur is folder ko select karein: `c:\Users\yash9\OneDrive\Desktop\quizeApp`.
3. Gradle sync complete hone de.
4. Apna phone USB debugging se connect karein ya Emulator start karke **Run (Green Play Button)** dabayein.
5. Phone me app open hone ke baad:
   - **Accessibility Service** enable karein ("Quiz Lock" ON karein).
   - **Display Over Other Apps** permission grant karein.
   - Jin apps ko lock karna hai unka switch ON kar dein!
