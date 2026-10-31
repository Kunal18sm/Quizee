/**
 * QUIZEE WEB TESTS PORTAL & TEST PLAYER ENGINE
 * Clean, Minimal & High-Performance
 */

(function () {
    'use strict';

    const CHUNK_SIZE = 100;
    const SUBJECT_CONFIG = [
        { key: 'english', name: 'English', icon: '📖' },
        { key: 'static_gk', name: 'Static GK', icon: '🏛️' },
        { key: 'history', name: 'History', icon: '🏺' },
        { key: 'geography', name: 'Geography', icon: '🌍' },
        { key: 'biology', name: 'Biology', icon: '🧬' },
        { key: 'polity', name: 'Polity', icon: '⚖️' },
        { key: 'economics', name: 'Economics', icon: '📈' }
    ];

    // Global State - Default to English for a clean, uncluttered start!
    let QUIZEE_CURRICULUM = [];
    let portalState = {
        activeSubject: 'English',
        searchQuery: '',
        selectedTopics: new Set(),
        expandedTopics: new Set(),
        selectedMultiCount: 50
    };

    let activeTest = {
        isOpen: false,
        title: '',
        subtitle: '',
        setKey: null,
        questions: [],
        currentIndex: 0,
        score: 0,
        isAnswerLocked: false,
        isHindiMode: false,
        reviewItems: [],
        timerSeconds: 0,
        timerInterval: null
    };

    const translationCache = {};

    function cleanCategoryName(catName, subjectName) {
        if (!catName) return subjectName;
        if (catName.startsWith('English:')) return catName.replace('English:', '').trim();
        if (catName.startsWith('Modern History:')) return catName.replace('Modern History:', '').trim() + ' (Modern)';
        if (catName.startsWith('Medieval History:')) return catName.replace('Medieval History:', '').trim() + ' (Medieval)';
        if (catName.startsWith('Ancient History:')) return catName.replace('Ancient History:', '').trim() + ' (Ancient)';
        if (catName === subjectName) return `${subjectName} (All Topics)`;
        return catName.trim();
    }

    async function loadAllQuestions() {
        if (window.QUIZEE_ALL_QUESTIONS && Object.keys(window.QUIZEE_ALL_QUESTIONS).length > 0) {
            return window.QUIZEE_ALL_QUESTIONS;
        }
        const loaded = {};
        const fetchPromises = SUBJECT_CONFIG.map(async sub => {
            try {
                const res = await fetch(`data/${sub.key}_questions.json`);
                if (res.ok) {
                    loaded[sub.key] = await res.json();
                } else {
                    loaded[sub.key] = [];
                }
            } catch (e) {
                loaded[sub.key] = [];
            }
        });
        await Promise.all(fetchPromises);
        window.QUIZEE_ALL_QUESTIONS = loaded;
        return loaded;
    }

    function buildCurriculum(data) {
        const curriculum = [];

        SUBJECT_CONFIG.forEach(sub => {
            const qs = data[sub.key] || [];
            if (qs.length === 0) return;

            const categoryMap = new Map();
            qs.forEach(q => {
                const cat = (q.category && q.category.trim()) ? q.category.trim() : sub.name;
                if (!categoryMap.has(cat)) categoryMap.set(cat, []);
                categoryMap.get(cat).push(q);
            });

            const topics = [];
            categoryMap.forEach((qList, catName) => {
                const displayName = cleanCategoryName(catName, sub.name);
                const sanitizedCat = catName.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase();
                const topicId = `topic_${sub.key}_${sanitizedCat}`;

                const numSets = Math.ceil(qList.length / CHUNK_SIZE);
                const sets = [];

                for (let i = 0; i < numSets; i++) {
                    const startIndex = i * CHUNK_SIZE;
                    const endIndex = Math.min(startIndex + CHUNK_SIZE, qList.length);
                    const chunkQuestions = qList.slice(startIndex, endIndex);
                    const setNumber = i + 1;
                    const setKey = `set_${sub.name.toLowerCase()}_${sanitizedCat}_${setNumber}`;

                    const isCompleted = localStorage.getItem(`${setKey}_completed`) === 'true';
                    const score = parseInt(localStorage.getItem(`${setKey}_score`) || '0', 10);
                    const totalAttempted = parseInt(localStorage.getItem(`${setKey}_total`) || `${chunkQuestions.length}`, 10);
                    const accuracy = (isCompleted && totalAttempted > 0) ? Math.min(100, Math.round((score / totalAttempted) * 100)) : 0;

                    let statusBadge = '';
                    if (isCompleted) {
                        if (accuracy >= 80) statusBadge = `🏆 ${accuracy}%`;
                        else if (accuracy >= 50) statusBadge = `🎯 ${accuracy}%`;
                        else statusBadge = `⚠️ ${accuracy}%`;
                    }

                    sets.push({
                        setNumber,
                        setKey,
                        subjectName: sub.name,
                        topicDisplayName: displayName,
                        categoryName: catName,
                        startRange: startIndex + 1,
                        endRange: endIndex,
                        totalQuestions: chunkQuestions.length,
                        questions: chunkQuestions,
                        isCompleted,
                        score,
                        totalAttempted,
                        accuracy,
                        statusBadge
                    });
                }

                const completedSets = sets.filter(s => s.isCompleted).length;
                const masteredSets = sets.filter(s => s.isCompleted && s.accuracy >= 80).length;
                const progressPct = sets.length > 0 ? Math.round((completedSets / sets.length) * 100) : 0;

                topics.push({
                    topicId,
                    subjectKey: sub.key,
                    subjectName: sub.name,
                    categoryName: catName,
                    displayName,
                    totalQuestions: qList.length,
                    questions: qList,
                    sets,
                    completedSets,
                    masteredSets,
                    progressPct
                });
            });

            curriculum.push({
                subjectKey: sub.key,
                name: sub.name,
                icon: sub.icon,
                totalQuestions: qs.length,
                totalSets: topics.reduce((acc, t) => acc + t.sets.length, 0),
                topics
            });
        });

        return curriculum;
    }

    // =========================================================
    // PORTAL RENDERING
    // =========================================================

    function renderSubjectTabs() {
        const container = document.getElementById('portalSubjectTabs');
        if (!container) return;

        let totalSetsAll = 0;
        QUIZEE_CURRICULUM.forEach(s => { totalSetsAll += s.totalSets; });

        // English first, then others, and All at the end
        const tabs = [
            ...QUIZEE_CURRICULUM.map(s => ({
                name: s.name,
                icon: s.icon,
                count: `${s.totalSets} Sets`
            })),
            { name: 'All', icon: '✨', count: `${totalSetsAll} Sets` }
        ];

        container.innerHTML = tabs.map(tab => `
            <button class="subject-tab-btn ${portalState.activeSubject === tab.name ? 'active' : ''}" data-subject="${tab.name}">
                <span>${tab.icon}</span>
                <span>${tab.name}</span>
                <span class="tab-count-badge">${tab.count}</span>
            </button>
        `).join('');

        container.querySelectorAll('.subject-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                portalState.activeSubject = btn.getAttribute('data-subject');
                renderSubjectTabs();
                renderTopicsGrid();
            });
        });
    }

    function renderLastPracticedBanner() {
        const banner = document.getElementById('portalResumeBanner');
        if (!banner) return;

        const raw = localStorage.getItem('quizee_last_practiced');
        if (!raw) {
            banner.style.display = 'none';
            return;
        }

        try {
            const data = JSON.parse(raw);
            const acc = data.total > 0 ? Math.round((data.score / data.total) * 100) : 0;

            banner.innerHTML = `
                <div class="resume-info">
                    <div class="resume-icon-circle">🎯</div>
                    <div>
                        <div class="resume-title">Resume: ${data.title}</div>
                        <div class="resume-meta">Last Score: <b>${data.score}/${data.total}</b> (${acc}%)</div>
                    </div>
                </div>
                <button class="btn-resume" id="btnResumeLast">
                    <span>Retest Set 🔁</span>
                </button>
            `;
            banner.style.display = 'flex';

            const resumeBtn = document.getElementById('btnResumeLast');
            if (resumeBtn) {
                resumeBtn.addEventListener('click', () => {
                    findAndStartSet(data.setKey);
                });
            }
        } catch (e) {
            banner.style.display = 'none';
        }
    }

    function renderTopicsGrid() {
        const grid = document.getElementById('portalTopicsGrid');
        if (!grid) return;

        const filtered = getFilteredTopics();

        if (filtered.length === 0) {
            grid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align:center; padding: 40px 20px; background:#141c2b; border:1px dashed rgba(255,255,255,0.1); border-radius:16px;">
                    <div style="font-size:32px; margin-bottom:8px;">🔍</div>
                    <div style="font-size:15px; font-weight:700; color:#f8fafc;">No matching topics found</div>
                    <div style="font-size:13px; color:#94a3b8; margin-top:4px;">Try typing a different search keyword.</div>
                </div>
            `;
            return;
        }

        grid.innerHTML = filtered.map(t => {
            const isChecked = portalState.selectedTopics.has(t.topicId);
            const isExp = portalState.expandedTopics.has(t.topicId);

            let completionPillClass = 'completion-unattempted';
            let completionPillText = `${t.completedSets}/${t.sets.length} Done`;
            if (t.completedSets === t.sets.length && t.sets.length > 0) {
                completionPillClass = t.masteredSets === t.sets.length ? 'completion-mastered' : 'completion-good';
                completionPillText = `${t.progressPct}% Done`;
            } else if (t.completedSets > 0) {
                completionPillClass = 'completion-progress';
                completionPillText = `${t.completedSets}/${t.sets.length} Sets`;
            }

            const setsHtml = t.sets.map(s => {
                const isUnder50 = s.totalQuestions <= 50;
                return `
                    <div class="web-set-card" id="set_card_${s.setKey}">
                        <div class="set-card-left">
                            <span class="set-card-title">Set ${s.setNumber}</span>
                            <span class="set-range-tag">(${s.startRange}–${s.endRange})</span>
                            ${s.statusBadge ? `<span class="set-badge-done">${s.statusBadge}</span>` : ''}
                        </div>
                        <div class="set-card-actions">
                            ${!isUnder50 ? `
                                <button class="btn-set-50" data-action="set50" data-setkey="${s.setKey}">
                                    ⚡ 50 Qs
                                </button>
                            ` : ''}
                            <button class="btn-set-full" data-action="setFull" data-setkey="${s.setKey}">
                                ${isUnder50 ? `Start (${s.totalQuestions})` : `Full (${s.totalQuestions})`}
                            </button>
                        </div>
                    </div>
                `;
            }).join('');

            return `
                <div class="web-topic-card ${isChecked ? 'selected' : ''}" id="topic_card_${t.topicId}">
                    <div class="topic-card-top">
                        <input type="checkbox" class="topic-select-checkbox" data-topicid="${t.topicId}" ${isChecked ? 'checked' : ''} title="Select for mixed test">
                        <div class="topic-main-info">
                            <div class="topic-card-title">${t.displayName}</div>
                            <div class="topic-card-meta">${t.totalQuestions} Questions • ${t.sets.length} Sets</div>
                        </div>
                        <span class="topic-completion-pill ${completionPillClass}">${completionPillText}</span>
                    </div>

                    <div class="topic-progress-track">
                        <div class="topic-progress-fill" style="width: ${t.progressPct}%;"></div>
                    </div>

                    <div class="topic-card-actions">
                        <button class="btn-toggle-sets" data-action="toggleSets" data-topicid="${t.topicId}">
                            <span>${isExp ? 'Hide Sets ▲' : `View Sets (${t.sets.length}) ▼`}</span>
                        </button>
                        <button class="btn-topic-drill" data-action="topicDrill" data-topicid="${t.topicId}">
                            ⚡ 50 Qs Drill
                        </button>
                    </div>

                    <div class="topic-sets-container" id="sets_box_${t.topicId}" style="display:${isExp ? 'flex' : 'none'};">
                        ${setsHtml}
                    </div>
                </div>
            `;
        }).join('');

        attachGridEventListeners(grid);
        updateMultiDockUI();
    }

    function getFilteredTopics() {
        let list = [];
        QUIZEE_CURRICULUM.forEach(sub => {
            if (portalState.activeSubject === 'All' || portalState.activeSubject === sub.name) {
                list = list.concat(sub.topics);
            }
        });

        if (portalState.searchQuery.trim()) {
            const q = portalState.searchQuery.toLowerCase().trim();
            list = list.filter(t => {
                return t.displayName.toLowerCase().includes(q) ||
                       t.categoryName.toLowerCase().includes(q) ||
                       t.subjectName.toLowerCase().includes(q);
            });
        }

        return list;
    }

    function attachGridEventListeners(container) {
        container.querySelectorAll('.topic-select-checkbox').forEach(cb => {
            cb.addEventListener('change', () => {
                const topicId = cb.getAttribute('data-topicid');
                if (cb.checked) {
                    portalState.selectedTopics.add(topicId);
                } else {
                    portalState.selectedTopics.delete(topicId);
                }
                const card = document.getElementById(`topic_card_${topicId}`);
                if (card) card.classList.toggle('selected', cb.checked);
                updateMultiDockUI();
            });
        });

        container.querySelectorAll('[data-action="toggleSets"]').forEach(btn => {
            btn.addEventListener('click', () => {
                const topicId = btn.getAttribute('data-topicid');
                const box = document.getElementById(`sets_box_${topicId}`);
                if (!box) return;

                const isExp = portalState.expandedTopics.has(topicId);
                const topic = findTopicById(topicId);
                const count = topic ? topic.sets.length : 0;
                if (isExp) {
                    portalState.expandedTopics.delete(topicId);
                    box.style.display = 'none';
                    btn.innerHTML = `<span>View Sets (${count}) ▼</span>`;
                } else {
                    portalState.expandedTopics.add(topicId);
                    box.style.display = 'flex';
                    btn.innerHTML = `<span>Hide Sets ▲</span>`;
                }
            });
        });

        container.querySelectorAll('[data-action="topicDrill"]').forEach(btn => {
            btn.addEventListener('click', () => {
                const topicId = btn.getAttribute('data-topicid');
                startTopicTest(topicId);
            });
        });

        container.querySelectorAll('[data-action="set50"]').forEach(btn => {
            btn.addEventListener('click', () => {
                const setKey = btn.getAttribute('data-setkey');
                findAndStartSet(setKey, true);
            });
        });

        container.querySelectorAll('[data-action="setFull"]').forEach(btn => {
            btn.addEventListener('click', () => {
                const setKey = btn.getAttribute('data-setkey');
                findAndStartSet(setKey, false);
            });
        });
    }

    function updateMultiDockUI() {
        const dock = document.getElementById('portalMultiDock');
        if (!dock) return;

        if (portalState.selectedTopics.size > 0) {
            let totalPool = 0;
            portalState.selectedTopics.forEach(id => {
                const t = findTopicById(id);
                if (t) totalPool += t.totalQuestions;
            });

            document.getElementById('dockSelectedCountText').textContent =
                `Selected: ${portalState.selectedTopics.size} Topics (${totalPool.toLocaleString()} Qs)`;
            document.getElementById('btnStartMultiMixed').textContent =
                `Start Mixed Test (${portalState.selectedMultiCount} Qs) 🚀`;
            dock.style.display = 'block';
        } else {
            dock.style.display = 'none';
        }
    }

    function findTopicById(topicId) {
        for (const sub of QUIZEE_CURRICULUM) {
            for (const t of sub.topics) {
                if (t.topicId === topicId) return t;
            }
        }
        return null;
    }

    function findAndStartSet(setKey, is50Count = false) {
        for (const sub of QUIZEE_CURRICULUM) {
            for (const t of sub.topics) {
                for (const s of t.sets) {
                    if (s.setKey === setKey) {
                        const count = is50Count ? Math.min(50, s.questions.length) : s.questions.length;
                        const qs = is50Count ? s.questions.slice(0, 50) : s.questions;
                        const title = `${t.displayName} • Set ${s.setNumber}`;
                        const subtitle = is50Count ? `50 Questions Drill` : `${s.totalQuestions} Questions Set`;
                        launchWebTest(qs, title, subtitle, s.setKey);
                        return;
                    }
                }
            }
        }
    }

    function startTopicTest(topicId) {
        const t = findTopicById(topicId);
        if (!t) return;
        const shuffled = [...t.questions].sort(() => 0.5 - Math.random());
        const count = Math.min(50, shuffled.length);
        const selected = shuffled.slice(0, count);
        launchWebTest(selected, `${t.displayName} • Rapid Drill`, `50 Questions Drill`, null);
    }

    function startCustomMixedTest() {
        if (portalState.selectedTopics.size === 0) return;

        let combined = [];
        portalState.selectedTopics.forEach(id => {
            const t = findTopicById(id);
            if (t) combined = combined.concat(t.questions);
        });

        if (combined.length === 0) return;
        const shuffled = [...combined].sort(() => 0.5 - Math.random());
        const count = Math.min(portalState.selectedMultiCount, shuffled.length);
        const selected = shuffled.slice(0, count);

        launchWebTest(
            selected,
            `Mixed Mock Test`,
            `${count} Questions from ${portalState.selectedTopics.size} Topics`,
            null
        );
    }

    // =========================================================
    // WEB TEST PLAYER
    // =========================================================

    function launchWebTest(questions, title, subtitle, setKey) {
        if (!questions || questions.length === 0) return;

        activeTest.isOpen = true;
        activeTest.title = title;
        activeTest.subtitle = subtitle;
        activeTest.setKey = setKey;
        activeTest.questions = questions;
        activeTest.currentIndex = 0;
        activeTest.score = 0;
        activeTest.isAnswerLocked = false;
        activeTest.isHindiMode = false;
        activeTest.reviewItems = [];
        activeTest.timerSeconds = 0;

        clearInterval(activeTest.timerInterval);
        activeTest.timerInterval = setInterval(() => {
            activeTest.timerSeconds++;
        }, 1000);

        const modal = document.getElementById('webTestModal');
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        document.getElementById('webTestActiveBody').style.display = 'flex';
        document.getElementById('webTestFooter').style.display = 'flex';
        document.getElementById('webTestResultsView').style.display = 'none';

        document.getElementById('webTestHeaderTitle').textContent = title;
        document.getElementById('btnTestTranslate').classList.remove('active-hindi');
        document.getElementById('btnTestTranslate').textContent = 'अ / A Hindi';

        displayActiveQuestion(0);
    }

    function displayActiveQuestion(index) {
        if (index < 0 || index >= activeTest.questions.length) return;

        activeTest.currentIndex = index;
        activeTest.isAnswerLocked = false;
        const q = activeTest.questions[index];

        const progressEl = document.getElementById('webTestHeaderProgress');
        if (progressEl) progressEl.textContent = `Q ${index + 1} of ${activeTest.questions.length}`;

        const scorePill = document.getElementById('webTestScorePill');
        if (scorePill) scorePill.textContent = `Score: ${activeTest.score}`;

        const fillEl = document.getElementById('webTestProgressFill');
        if (fillEl) {
            const pct = Math.round(((index + 1) / activeTest.questions.length) * 100);
            fillEl.style.width = `${pct}%`;
        }

        const catTag = document.getElementById('webQuestionCategory');
        if (catTag) catTag.textContent = q.category || 'SSC Exam';

        renderQuestionTextAndOptions(q);

        const skipBtn = document.getElementById('btnTestSkip');
        if (skipBtn) skipBtn.style.display = 'inline-block';

        const nextBtn = document.getElementById('btnTestNext');
        if (nextBtn) {
            nextBtn.disabled = true;
            nextBtn.textContent = (index === activeTest.questions.length - 1) ? 'View Results 🏆' : 'Next ➔';
        }

        const scrollBody = document.getElementById('webTestActiveBody');
        if (scrollBody) scrollBody.scrollTop = 0;
    }

    function renderQuestionTextAndOptions(q) {
        const qTextEl = document.getElementById('webQuestionText');
        const optionsBox = document.getElementById('webOptionsContainer');
        if (!qTextEl || !optionsBox) return;

        let displayQ = q.question;
        let displayOpts = [...q.options];

        if (activeTest.isHindiMode) {
            if (translationCache[q.id]) {
                displayQ = translationCache[q.id].question;
                displayOpts = translationCache[q.id].options;
            } else {
                const embedded = extractEmbeddedHindiQuestion(q);
                if (embedded) {
                    displayQ = embedded.question;
                    displayOpts = embedded.options;
                }
            }
        }

        qTextEl.textContent = displayQ;
        optionsBox.innerHTML = '';

        const badges = ['A', 'B', 'C', 'D'];
        displayOpts.forEach((optText, i) => {
            const btn = document.createElement('button');
            btn.className = 'web-option-btn';
            btn.setAttribute('data-index', i);
            btn.innerHTML = `
                <span class="web-option-badge">${badges[i]}</span>
                <span class="web-option-text">${optText}</span>
                <span class="web-option-feedback-icon" id="feedback_icon_${i}"></span>
            `;
            btn.addEventListener('click', () => handleOptionSelected(i));
            optionsBox.appendChild(btn);
        });
    }

    function extractEmbeddedHindiQuestion(q) {
        const qParts = q.question.split('/');
        let hindiQ = q.question;
        if (qParts.length > 1 && /[ऀ-ॿ]/.test(qParts[1])) {
            hindiQ = qParts[1].trim();
        }

        const hindiOpts = q.options.map(opt => {
            const parts = opt.split('/');
            if (parts.length > 1 && /[ऀ-ॿ]/.test(parts[1])) {
                return parts[1].trim();
            }
            return opt;
        });

        return { question: hindiQ, options: hindiOpts };
    }

    function handleOptionSelected(selectedIndex) {
        if (activeTest.isAnswerLocked) return;
        activeTest.isAnswerLocked = true;

        const currentQ = activeTest.questions[activeTest.currentIndex];
        const isCorrect = (selectedIndex === currentQ.correctIndex);

        const allButtons = document.querySelectorAll('.web-option-btn');
        allButtons.forEach(b => b.disabled = true);

        const chosenBtn = allButtons[selectedIndex];
        if (isCorrect) {
            activeTest.score++;
            document.getElementById('webTestScorePill').textContent = `Score: ${activeTest.score}`;
            if (chosenBtn) {
                chosenBtn.classList.add('opt-correct');
                const icon = chosenBtn.querySelector('.web-option-feedback-icon');
                if (icon) icon.textContent = '✓';
            }
        } else {
            if (chosenBtn) {
                chosenBtn.classList.add('opt-wrong');
                const icon = chosenBtn.querySelector('.web-option-feedback-icon');
                if (icon) icon.textContent = '✕';
            }
            if (currentQ.correctIndex < allButtons.length) {
                const correctBtn = allButtons[currentQ.correctIndex];
                correctBtn.classList.add('opt-correct');
                const icon = correctBtn.querySelector('.web-option-feedback-icon');
                if (icon) icon.textContent = '✓';
            }
        }

        activeTest.reviewItems.push({
            qNum: activeTest.currentIndex + 1,
            question: currentQ,
            selectedIndex,
            isCorrect
        });

        const skipBtn = document.getElementById('btnTestSkip');
        if (skipBtn) skipBtn.style.display = 'none';

        const nextBtn = document.getElementById('btnTestNext');
        if (nextBtn) nextBtn.disabled = false;
    }

    function handleSkipQuestion() {
        if (activeTest.isAnswerLocked) return;
        activeTest.isAnswerLocked = true;

        const currentQ = activeTest.questions[activeTest.currentIndex];
        const allButtons = document.querySelectorAll('.web-option-btn');
        allButtons.forEach(b => b.disabled = true);

        if (currentQ.correctIndex < allButtons.length) {
            const correctBtn = allButtons[currentQ.correctIndex];
            correctBtn.classList.add('opt-correct');
            const icon = correctBtn.querySelector('.web-option-feedback-icon');
            if (icon) icon.textContent = '✓';
        }

        activeTest.reviewItems.push({
            qNum: activeTest.currentIndex + 1,
            question: currentQ,
            selectedIndex: -1,
            isCorrect: false
        });

        const skipBtn = document.getElementById('btnTestSkip');
        if (skipBtn) skipBtn.style.display = 'none';

        const nextBtn = document.getElementById('btnTestNext');
        if (nextBtn) nextBtn.disabled = false;
    }

    function handleNextQuestion() {
        if (activeTest.currentIndex + 1 < activeTest.questions.length) {
            displayActiveQuestion(activeTest.currentIndex + 1);
        } else {
            showWebTestResults();
        }
    }

    function showWebTestResults() {
        clearInterval(activeTest.timerInterval);

        document.getElementById('webTestActiveBody').style.display = 'none';
        document.getElementById('webTestFooter').style.display = 'none';
        const resultsView = document.getElementById('webTestResultsView');
        resultsView.style.display = 'block';

        const total = activeTest.questions.length;
        const correct = activeTest.score;
        const wrong = activeTest.reviewItems.filter(r => !r.isCorrect && r.selectedIndex !== -1).length;
        const skipped = activeTest.reviewItems.filter(r => r.selectedIndex === -1).length;
        const percentage = total > 0 ? Math.round((correct / total) * 100) : 0;

        document.getElementById('resultsMainTitle').textContent = `Test Completed! 🎉`;
        document.getElementById('resultsSubtitle').textContent = activeTest.title;
        document.getElementById('resultsScoreDigits').textContent = `${correct} / ${total}`;
        document.getElementById('resultsAccuracyPill').textContent = `${percentage}% Accuracy`;
        document.getElementById('resultsMetricCorrect').textContent = correct;
        document.getElementById('resultsMetricWrong').textContent = wrong;
        document.getElementById('resultsMetricSkipped').textContent = skipped;

        let badgeCallout = '';
        if (percentage >= 80) badgeCallout = `🏆 Mastered (${percentage}%)`;
        else if (percentage >= 50) badgeCallout = `🎯 Good Job (${percentage}%)`;
        else badgeCallout = `⚠️ Needs Practice (${percentage}%)`;
        document.getElementById('resultsBadgeCallout').textContent = badgeCallout;

        if (activeTest.setKey) {
            localStorage.setItem(`${activeTest.setKey}_completed`, 'true');
            localStorage.setItem(`${activeTest.setKey}_score`, correct.toString());
            localStorage.setItem(`${activeTest.setKey}_total`, total.toString());
            localStorage.setItem(`${activeTest.setKey}_timestamp`, Date.now().toString());

            localStorage.setItem('quizee_last_practiced', JSON.stringify({
                setKey: activeTest.setKey,
                title: activeTest.title,
                score: correct,
                total: total,
                timestamp: Date.now()
            }));

            QUIZEE_CURRICULUM = buildCurriculum(window.QUIZEE_ALL_QUESTIONS);
            renderLastPracticedBanner();
            renderTopicsGrid();
        }

        renderReviewList();
    }

    function renderReviewList() {
        const accordion = document.getElementById('resultsReviewList');
        if (!accordion) return;

        const badges = ['A', 'B', 'C', 'D'];
        accordion.innerHTML = activeTest.reviewItems.map(item => {
            const q = item.question;
            let statusClass = item.isCorrect ? 'is-correct' : (item.selectedIndex === -1 ? 'is-skipped' : 'is-wrong');
            let statusText = item.isCorrect ? '✓ Correct' : (item.selectedIndex === -1 ? 'Skipped' : '✕ Wrong');
            let statusColor = item.isCorrect ? '#10b981' : (item.selectedIndex === -1 ? '#f59e0b' : '#ef4444');

            let yourChoiceHtml = '';
            if (item.selectedIndex !== -1) {
                yourChoiceHtml = `
                    <div class="review-item-choice ${item.isCorrect ? 'choice-correct' : 'choice-your-wrong'}">
                        <b>Your Answer (${badges[item.selectedIndex]}):</b> ${q.options[item.selectedIndex]}
                    </div>
                `;
            } else {
                yourChoiceHtml = `
                    <div class="review-item-choice choice-your-wrong" style="color:#f59e0b;">
                        <b>Your Answer:</b> Skipped
                    </div>
                `;
            }

            const correctChoiceHtml = `
                <div class="review-item-choice choice-correct">
                    <b>Correct Answer (${badges[q.correctIndex]}):</b> ${q.options[q.correctIndex]}
                </div>
            `;

            return `
                <div class="review-item-card ${statusClass}">
                    <div class="review-item-header">
                        <span style="color:#94a3b8;">Question ${item.qNum}</span>
                        <span style="color:${statusColor}; font-weight:700;">${statusText}</span>
                    </div>
                    <div class="review-item-qtext">${q.question}</div>
                    ${yourChoiceHtml}
                    ${!item.isCorrect ? correctChoiceHtml : ''}
                </div>
            `;
        }).join('');
    }

    async function toggleTestLanguage() {
        activeTest.isHindiMode = !activeTest.isHindiMode;
        const btn = document.getElementById('btnTestTranslate');
        if (btn) {
            btn.classList.toggle('active-hindi', activeTest.isHindiMode);
            btn.textContent = activeTest.isHindiMode ? 'A / अ English' : 'अ / A Hindi';
        }

        const currentQ = activeTest.questions[activeTest.currentIndex];
        if (!currentQ) return;

        if (activeTest.isHindiMode && !translationCache[currentQ.id]) {
            const embedded = extractEmbeddedHindiQuestion(currentQ);
            if (embedded && /[ऀ-ॿ]/.test(embedded.question)) {
                translationCache[currentQ.id] = embedded;
            } else {
                try {
                    btn.textContent = 'Translating...';
                    const trQ = await fetchGoogleTranslate(currentQ.question);
                    const trOpts = await Promise.all(currentQ.options.map(o => fetchGoogleTranslate(o)));
                    translationCache[currentQ.id] = { question: trQ, options: trOpts };
                    btn.textContent = 'A / अ English';
                } catch (e) {
                    btn.textContent = 'A / अ English';
                }
            }
        }

        renderQuestionTextAndOptions(currentQ);
    }

    async function fetchGoogleTranslate(text) {
        try {
            const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=hi&dt=t&q=${encodeURIComponent(text)}`;
            const res = await fetch(url);
            if (res.ok) {
                const data = await res.json();
                if (data && data[0]) {
                    return data[0].map(s => s[0]).join('');
                }
            }
        } catch (e) {}
        return text;
    }

    function confirmExitTest() {
        if (!activeTest.isOpen) return;
        if (document.getElementById('webTestResultsView').style.display === 'block') {
            closeWebTest();
            return;
        }
        const confirmModal = document.getElementById('webExitConfirmModal');
        if (confirmModal) confirmModal.classList.add('active');
    }

    function closeWebTest() {
        clearInterval(activeTest.timerInterval);
        activeTest.isOpen = false;
        const modal = document.getElementById('webTestModal');
        if (modal) modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    // Window Exports
    window.clearTopicSelections = function () {
        portalState.selectedTopics.clear();
        renderTopicsGrid();
    };

    window.selectMultiCount = function (count) {
        portalState.selectedMultiCount = count;
        document.querySelectorAll('.dock-chip').forEach(c => {
            c.classList.toggle('active', parseInt(c.textContent) === count);
        });
        const btn = document.getElementById('btnStartMultiMixed');
        if (btn) btn.textContent = `Start Mixed Test (${count} Qs) 🚀`;
    };

    window.startCustomMixedTest = startCustomMixedTest;
    window.confirmExitTest = confirmExitTest;
    window.closeWebTest = closeWebTest;
    window.toggleTestLanguage = toggleTestLanguage;
    window.handleSkipQuestion = handleSkipQuestion;
    window.handleNextQuestion = handleNextQuestion;

    window.toggleResultsReview = function () {
        const accordion = document.getElementById('resultsReviewList');
        const btn = document.getElementById('btnToggleReview');
        if (!accordion || !btn) return;
        const isShown = accordion.style.display === 'block';
        accordion.style.display = isShown ? 'none' : 'block';
        btn.textContent = isShown ? 'View Question Review 📋' : 'Hide Review ✕';
    };

    window.retestCurrentSet = function () {
        if (!activeTest.questions || activeTest.questions.length === 0) return;
        launchWebTest(activeTest.questions, activeTest.title, activeTest.subtitle, activeTest.setKey);
    };

    // Keyboard Shortcuts
    document.addEventListener('keydown', (e) => {
        if (!activeTest.isOpen) return;
        if (document.getElementById('webTestResultsView').style.display === 'block') return;

        if (e.key === 'Escape') {
            confirmExitTest();
            return;
        }

        if (!activeTest.isAnswerLocked) {
            if (e.key === '1' || e.key === 'a' || e.key === 'A') handleOptionSelected(0);
            else if (e.key === '2' || e.key === 'b' || e.key === 'B') handleOptionSelected(1);
            else if (e.key === '3' || e.key === 'c' || e.key === 'C') handleOptionSelected(2);
            else if (e.key === '4' || e.key === 'd' || e.key === 'D') handleOptionSelected(3);
            else if (e.key === 's' || e.key === 'S') handleSkipQuestion();
        } else {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleNextQuestion();
            }
        }
    });

    async function initPortal() {
        const questionsData = await loadAllQuestions();
        QUIZEE_CURRICULUM = buildCurriculum(questionsData);

        renderSubjectTabs();
        renderLastPracticedBanner();
        renderTopicsGrid();

        const searchInput = document.getElementById('portalSearchInput');
        const clearBtn = document.getElementById('portalSearchClear');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                portalState.searchQuery = e.target.value;
                if (clearBtn) clearBtn.style.display = e.target.value ? 'block' : 'none';
                renderTopicsGrid();
            });
        }
        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                searchInput.value = '';
                portalState.searchQuery = '';
                clearBtn.style.display = 'none';
                renderTopicsGrid();
            });
        }

        const btnConfirmExit = document.getElementById('btnConfirmExit');
        const btnConfirmStay = document.getElementById('btnConfirmStay');
        if (btnConfirmExit) {
            btnConfirmExit.addEventListener('click', () => {
                const confirmModal = document.getElementById('webExitConfirmModal');
                if (confirmModal) confirmModal.classList.remove('active');
                closeWebTest();
            });
        }
        if (btnConfirmStay) {
            btnConfirmStay.addEventListener('click', () => {
                const confirmModal = document.getElementById('webExitConfirmModal');
                if (confirmModal) confirmModal.classList.remove('active');
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPortal);
    } else {
        initPortal();
    }

})();
