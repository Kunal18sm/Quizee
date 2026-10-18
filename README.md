# 📱 Quizee - SSC CGL Daily Mastery & App Locker

An intelligent Android App Locker & Microlearning System designed for competitive exams (SSC CGL, CHSL, CPO, Railways, State Exams). Instead of completely random questions, Quizee automatically curates a structured **Daily Target Bank (30 or 20 Questions)** following a sequential, chapter-wise curriculum. Every time you open a locked app (like Instagram, YouTube, etc.), it tests you on these 30 questions with **Mistake-Priority Spaced Repetition**, permanently committing high-yield questions to memory!

---

## ✨ Core Features & Functionality

### 1. 🎯 Daily Structured Learning Engine (30 or 20 Questions/Day)
- **Zero Randomness in Bank Selection:** Questions are not picked randomly. Every subject maintains a persistent pointer and advances sequentially through the syllabus chapter-by-chapter.
- **Balanced SSC CGL Daily Quota (30 Qs / Day Mode):**
  - 📖 **English Vocab (8 Qs):** Synonyms, Antonyms, OWS, Idioms, Spellings, Phrasal Verbs
  - 🏆 **Static GK (6 Qs):** Dances, Art & Culture, Books/Authors, 106 National Parks, Dams
  - 📜 **Indian History (5 Qs):** Ancient $\to$ Medieval $\to$ Modern chronological flow
  - 🏛️ **Indian Polity (3 Qs):** Constitution, Articles, Amendments, Parliament
  - 🧬 **Biology (3 Qs):** Human Physiology, Diseases, Cell, Plant Kingdom
  - 🌍 **Geography (3 Qs):** Rivers, Mountains, Climate, Physical Geography
  - 📈 **Economics (2 Qs):** Banking, Inflation, Five Year Plans, Core Terms
- **Customizable Target:** Toggle between **30 Qs/Day (Fast-Track)** and **20 Qs/Day (Relaxed)** directly from the home screen dashboard!
- **Automatic Midnight Rollover:** Advances to the next day's questions automatically every midnight, with manual "Next Day" override option.

### 2. 🧠 Smart Mistake-Priority Sampling (Lock Screen Quizzes)
- Whenever a locked app is launched, Quizee displays **5 MCQs** sampled from **Today's 30-Question Bank**.
- **Weak-Point Auto-Detection:** If you answer a question incorrectly, Quizee immediately prioritizes that question in subsequent lock quizzes until you answer it correctly!
- **Unseen Question Priority:** Ensures all 30 questions are presented before repeating mastered ones.

### 3. 👁️ Today's Bank Review & Progress Meter
- **Dashboard Progress Bar:** Shows live count of *"X / 30 Mastered"* and current *"🔥 Day Streak"*.
- **Review Today's Bank:** Tap *"Review Today's 30 Qs"* to view all 30 questions with options, highlighted correct answers, and status badges (`Mastered`, `Needs Revision`, `In Progress`, `Unseen`).

### 4. 📚 100% High-Yield SSC CGL Question Bank (6,517 MCQs)
Filtered and pruned of all low-yield junk (foreign words, obscure sanctuaries, elementary school full forms, single-term fillers):
- **English Vocabulary:** 2,620 MCQs (BlackBook PYQs)
- **Static GK:** 1,554 MCQs (106 National Parks, Classical Dances, Famous Books, Superlatives)
- **Indian History:** 984 MCQs (Complete Chronology)
- **Geography:** 503 MCQs
- **Biology:** 317 MCQs
- **Indian Polity:** 271 MCQs
- **Economics:** 268 MCQs

### 5. 🛡️ Rock-Solid App Locking & Floating Overlay
- **Instant Accessibility Open Detector:** Catches target apps immediately.
- **Floating Overlay:** Seamless quiz card displays on top of target app.
- **Haptic Feedback & Instant Visuals:** Green for correct, Red for wrong with instant reveal.
- **Screen-Off Session Management:** Once unlocked by passing the quiz (3/5 score), the app stays unlocked until screen turns off.

---

## 📂 Project Structure

```
quizeApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   ├── english_questions.json      <-- 2,620 Vocab MCQs
│   │   │   ├── static_gk_questions.json    <-- 1,554 High-Yield Static GK MCQs
│   │   │   ├── history_questions.json      <-- 984 History MCQs
│   │   │   ├── geography_questions.json    <-- 503 Geography MCQs
│   │   │   ├── biology_questions.json      <-- 317 Biology MCQs
│   │   │   ├── polity_questions.json       <-- 271 Polity MCQs
│   │   │   └── economics_questions.json    <-- 268 Economics MCQs
│   │   ├── java/com/quizlock/app/
│   │   │   ├── MainActivity.kt             <-- Daily Target Dashboard & App Selector
│   │   │   ├── data/
│   │   │   │   ├── DailyStudyManager.kt    <-- Sequential Curriculum & Spaced Repetition Engine
│   │   │   │   ├── QuestionRepository.kt   <-- 7 Subject Banks Loader
│   │   │   │   ├── Question.kt             <-- MCQ Model
│   │   │   │   └── PreferencesManager.kt   <-- Lock Settings & Sessions
│   │   │   ├── service/
│   │   │   │   ├── QuizOverlayManager.kt   <-- Floating Lock Card
│   │   │   │   ├── AppLockAccessibilityService.kt <-- Instant App Monitor
│   │   │   │   └── AppLockForegroundService.kt
│   │   │   └── ui/
│   │   │       ├── DailyReviewAdapter.kt   <-- Review Today's Bank List Adapter
│   │   │       ├── QuizLockActivity.kt     <-- Full Screen Quiz Activity
│   │   │       ├── QuestionManageActivity.kt
│   │   │       └── AppListAdapter.kt
│   │   └── res/                            <-- Layouts, Drawables, Styles
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 How to Run & Build

1. Open in **Android Studio** (`File > Open` -> select `quizeApp`).
2. Sync Gradle files and connect your Android device or start an emulator.
3. Tap **Run** (Green Play Button).
4. Or push to GitHub to automatically build and release the APK via **GitHub Actions** (`.github/workflows/build-and-deploy.yml`).
