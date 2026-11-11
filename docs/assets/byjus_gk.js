/**
 * BYJU'S Static GK Special Quiz Portal & Player Engine
 * Quizee Interactive Exam Engine
 */

(function () {
    'use strict';

    // State
    let allTopics = [];
    let allQuestionsMap = {};
    let activeCategory = 'All';
    let searchQuery = '';

    let activeQuiz = {
        isOpen: false,
        topicId: null,
        topicTitle: '',
        category: '',
        questions: [],
        currentIndex: 0,
        score: 0,
        isAnswerLocked: false,
        isHindiMode: false,
        reviewItems: [],
        timerSeconds: 0,
        timerInterval: null
    };

    // Category mapping & icons
    const CATEGORIES = [
        { key: 'All', name: 'All Topics', icon: '✨' },
        { key: 'Polity & Schemes', name: 'Polity & Schemes', icon: '🏛️' },
        { key: 'World Geography', name: 'World Geography', icon: '🌍' },
        { key: 'India Geography', name: 'India Geography & Sites', icon: '🇮🇳' },
        { key: 'Banking & Economy', name: 'Banking & Economy', icon: '💰' },
        { key: 'Sports & Tournaments', name: 'Sports & Tournaments', icon: '🏆' },
        { key: 'Awards & Defense', name: 'Awards & Defense', icon: '🎖️' },
        { key: 'Science & Awareness', name: 'Science & Awareness', icon: '🔬' }
    ];

    function init() {
        if (!window.BYJUS_GK_DATA) {
            console.error("BYJUS_GK_DATA not loaded");
            return;
        }

        allTopics = window.BYJUS_GK_DATA.topics || [];
        allQuestionsMap = window.BYJUS_GK_DATA.questions || {};

        renderCategoryPills();
        renderTopicCards();
        updateOverallStats();
        setupEventListeners();
    }

    function renderCategoryPills() {
        const container = document.getElementById('byjusCategoryTabs');
        if (!container) return;

        let html = '';
        CATEGORIES.forEach(cat => {
            const count = cat.key === 'All' 
                ? allTopics.length 
                : allTopics.filter(t => t.category === cat.key).length;
            
            const isActive = activeCategory === cat.key;
            html += `
                <button class="byjus-cat-pill ${isActive ? 'active' : ''}" onclick="window.byjusSelectCategory('${cat.key}')">
                    <span>${cat.icon}</span>
                    <span>${cat.name}</span>
                    <span class="byjus-pill-count">${count}</span>
                </button>
            `;
        });
        container.innerHTML = html;
    }

    function renderTopicCards() {
        const grid = document.getElementById('byjusTopicGrid');
        if (!grid) return;

        const filtered = allTopics.filter(topic => {
            const matchesCat = activeCategory === 'All' || topic.category === activeCategory;
            const matchesQuery = !searchQuery || 
                topic.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                topic.category.toLowerCase().includes(searchQuery.toLowerCase());
            return matchesCat && matchesQuery;
        });

        const countHeader = document.getElementById('byjusVisibleCount');
        if (countHeader) {
            countHeader.textContent = `Showing ${filtered.length} of ${allTopics.length} Topics`;
        }

        if (filtered.length === 0) {
            grid.innerHTML = `
                <div class="byjus-empty-state">
                    <div style="font-size:36px; margin-bottom:12px;">🔍</div>
                    <h3 style="font-size:18px; color:#f8fafc; margin-bottom:6px;">No Topics Match "${searchQuery}"</h3>
                    <p style="color:#94a3b8; font-size:13px; margin-bottom:14px;">Try searching for "river", "capital", "temple", "bank", or select another category.</p>
                    <button class="byjus-btn-reset-filter" onclick="window.byjusResetFilters()">View All Topics</button>
                </div>
            `;
            return;
        }

        let html = '';
        filtered.forEach(topic => {
            const qList = allQuestionsMap[topic.id] || [];
            const count = qList.length;

            // Check localStorage progress
            const storageKey = `byjus_gk_${topic.id}`;
            const isCompleted = localStorage.getItem(`${storageKey}_completed`) === 'true';
            const bestScore = localStorage.getItem(`${storageKey}_score`);

            let statusBadge = `<span class="byjus-status-badge unattempted">Not Attempted</span>`;
            if (isCompleted && bestScore !== null) {
                const s = parseFloat(bestScore);
                const pct = Math.round((s / count) * 100);
                if (pct >= 80) {
                    statusBadge = `<span class="byjus-status-badge mastered">✓ Mastered (${s}/${count})</span>`;
                } else {
                    statusBadge = `<span class="byjus-status-badge passed">✓ Completed (${s}/${count})</span>`;
                }
            }

            html += `
                <div class="byjus-topic-card ${isCompleted ? 'card-completed' : ''}">
                    <div class="byjus-card-top">
                        <div class="byjus-card-icon-wrap">
                            <span class="byjus-card-icon">${topic.icon || '📌'}</span>
                            <span class="byjus-category-tag">${topic.category}</span>
                        </div>
                        ${statusBadge}
                    </div>

                    <h3 class="byjus-card-title">${topic.title}</h3>

                    <div class="byjus-card-meta">
                        <span class="byjus-meta-item">📝 <strong>${count}</strong> MCQs</span>
                        <div class="byjus-card-meta-links">
                            <button class="byjus-table-preview-btn" onclick="window.viewTopicStudyTable('${topic.id}')" title="View study table">
                                📖 Table
                            </button>
                            <a href="${topic.url}" target="_blank" rel="noopener" class="byjus-meta-link" title="BYJU'S Source Notes">
                                Notes ↗
                            </a>
                        </div>
                    </div>

                    <div class="byjus-card-actions">
                        <button class="byjus-btn-play" onclick="window.startByjusQuiz('${topic.id}', 'all')">
                            <span>Play Quiz (${count} Qs)</span>
                            <span>➔</span>
                        </button>
                    </div>
                </div>
            `;
        });

        grid.innerHTML = html;
    }

    function updateOverallStats() {
        let totalMastered = 0;
        let totalCompleted = 0;
        let totalQuestions = 0;

        allTopics.forEach(t => {
            const storageKey = `byjus_gk_${t.id}`;
            const qList = allQuestionsMap[t.id] || [];
            totalQuestions += qList.length;

            if (localStorage.getItem(`${storageKey}_completed`) === 'true') {
                totalCompleted++;
                const s = parseFloat(localStorage.getItem(`${storageKey}_score`) || 0);
                if (qList.length > 0 && (s / qList.length) >= 0.8) {
                    totalMastered++;
                }
            }
        });

        const statComp = document.getElementById('byjusStatCompleted');
        const statMastered = document.getElementById('byjusStatMastered');
        const statTotalQs = document.getElementById('byjusTotalQuestionsBadge');
        if (statComp) statComp.textContent = `${totalCompleted} / ${allTopics.length}`;
        if (statMastered) statMastered.textContent = `${totalMastered} Topics`;
        if (statTotalQs) statTotalQs.textContent = `${totalQuestions}+ Exam MCQs`;
    }

    function setupEventListeners() {
        const searchInput = document.getElementById('byjusSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                searchQuery = e.target.value.trim();
                renderTopicCards();
            });
        }
    }

    // Study Table Viewer Modal
    window.viewTopicStudyTable = function (topicId) {
        const topic = allTopics.find(t => t.id === topicId);
        if (!topic) return;

        const modal = document.getElementById('byjusTableModal');
        const titleEl = document.getElementById('byjusTableModalTitle');
        const bodyEl = document.getElementById('byjusTableModalBody');
        const linkEl = document.getElementById('byjusTableModalSourceLink');

        if (!modal || !bodyEl) return;

        titleEl.textContent = `${topic.icon || '📌'} ${topic.title} — Reference Table & Complete Facts`;
        if (linkEl) linkEl.href = topic.url;

        const tables = topic.studyTables || [];
        if (tables.length === 0) {
            bodyEl.innerHTML = `<div style="text-align:center; padding:30px; color:#94a3b8;">No table rows extracted. Please refer to <a href="${topic.url}" target="_blank" style="color:#6366f1;">BYJU'S Source Notes ↗</a>.</div>`;
        } else {
            let html = '';
            tables.forEach((tbl, tIdx) => {
                const headers = tbl.headers || [];
                const rows = tbl.rows || [];

                const cleanCell = (v) => {
                    let s = String(v || '').trim();
                    if (/'[a-zA-Z0-9]'/.test(s) || s.includes("''")) {
                        s = s.replace(/'/g, '');
                    }
                    return s.replace(/\ufffd/g, "'");
                };

                html += `
                    <div class="byjus-table-wrap">
                        <div class="byjus-table-label">Section Table #${tIdx + 1} (${rows.length} Items Listed)</div>
                        <div class="byjus-scrollable-table">
                            <table class="byjus-data-table">
                                <thead>
                                    <tr>
                                        ${headers.map(h => `<th>${cleanCell(h)}</th>`).join('')}
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows.map(row => `
                                        <tr>
                                            ${row.map(cell => `<td>${cleanCell(cell)}</td>`).join('')}
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                `;
            });
            bodyEl.innerHTML = html;
        }

        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    };

    window.closeTopicStudyTable = function () {
        const modal = document.getElementById('byjusTableModal');
        if (modal) modal.classList.remove('active');
        document.body.style.overflow = '';
    };

    // Window Exposure for UI clicks
    window.byjusSelectCategory = function (catKey) {
        activeCategory = catKey;
        renderCategoryPills();
        renderTopicCards();
    };

    window.byjusResetFilters = function () {
        activeCategory = 'All';
        searchQuery = '';
        const searchInput = document.getElementById('byjusSearchInput');
        if (searchInput) searchInput.value = '';
        renderCategoryPills();
        renderTopicCards();
    };

    window.startByjusQuiz = function (topicId, countMode = 'all') {
        const topic = allTopics.find(t => t.id === topicId);
        if (!topic) return;

        const qList = allQuestionsMap[topicId] || [];
        if (qList.length === 0) {
            alert('No questions available for this topic yet.');
            return;
        }

        let questions = JSON.parse(JSON.stringify(qList)).sort(() => 0.5 - Math.random());
        
        if (typeof countMode === 'number' && countMode < questions.length) {
            questions = questions.slice(0, countMode);
        }

        activeQuiz = {
            isOpen: true,
            topicId: topicId,
            topicTitle: `${topic.title} (${questions.length} Qs)`,
            category: topic.category,
            questions: questions,
            currentIndex: 0,
            score: 0,
            isAnswerLocked: false,
            isHindiMode: false,
            reviewItems: [],
            timerSeconds: 0,
            timerInterval: null
        };

        openTestModal();
        startTimer();
        renderCurrentQuestion();
    };

    window.startMegaMixedQuiz = function (count = 30) {
        let pool = [];
        for (const tid in allQuestionsMap) {
            pool.push(...allQuestionsMap[tid]);
        }

        if (pool.length === 0) return;

        pool = JSON.parse(JSON.stringify(pool)).sort(() => 0.5 - Math.random());
        const selected = pool.slice(0, Math.min(count, pool.length));

        activeQuiz = {
            isOpen: true,
            topicId: 'mega_mixed',
            topicTitle: `Mega Mixed BYJU'S Static GK (${selected.length} Qs)`,
            category: 'All 58 Topics Mixed',
            questions: selected,
            currentIndex: 0,
            score: 0,
            isAnswerLocked: false,
            isHindiMode: false,
            reviewItems: [],
            timerSeconds: 0,
            timerInterval: null
        };

        openTestModal();
        startTimer();
        renderCurrentQuestion();
    };

    function openTestModal() {
        const modal = document.getElementById('byjusQuizModal');
        if (!modal) return;

        modal.classList.add('active');
        document.body.style.overflow = 'hidden';

        document.getElementById('byjusActiveView').style.display = 'flex';
        document.getElementById('byjusResultsView').style.display = 'none';

        document.getElementById('byjusQuizTitle').textContent = activeQuiz.topicTitle;
        updateScorePill();
    }

    function startTimer() {
        if (activeQuiz.timerInterval) clearInterval(activeQuiz.timerInterval);
        activeQuiz.timerSeconds = 0;
        updateTimerDisplay();

        activeQuiz.timerInterval = setInterval(() => {
            activeQuiz.timerSeconds++;
            updateTimerDisplay();
        }, 1000);
    }

    function updateTimerDisplay() {
        const el = document.getElementById('byjusQuizTimer');
        if (!el) return;
        const m = Math.floor(activeQuiz.timerSeconds / 60);
        const s = activeQuiz.timerSeconds % 60;
        el.textContent = `⏱️ ${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
    }

    function updateScorePill() {
        const el = document.getElementById('byjusQuizScorePill');
        if (el) {
            el.textContent = `Score: ${activeQuiz.score}`;
        }
    }

    function renderCurrentQuestion() {
        const total = activeQuiz.questions.length;
        const currentNum = activeQuiz.currentIndex + 1;
        const q = activeQuiz.questions[activeQuiz.currentIndex];

        const progressEl = document.getElementById('byjusQuizProgressText');
        if (progressEl) progressEl.textContent = `Question ${currentNum} of ${total}`;

        const fillEl = document.getElementById('byjusQuizProgressFill');
        if (fillEl) {
            const pct = Math.round((currentNum / total) * 100);
            fillEl.style.width = `${pct}%`;
        }

        const catTag = document.getElementById('byjusQuestionCategoryTag');
        if (catTag) {
            catTag.textContent = `${q.category || activeQuiz.category} • ${q.topicTitle || activeQuiz.topicTitle}`;
        }

        const qTextEl = document.getElementById('byjusQuestionText');
        const showHindi = activeQuiz.isHindiMode && q.hindiQuestion;
        qTextEl.textContent = showHindi ? q.hindiQuestion : q.question;

        activeQuiz.isAnswerLocked = false;

        const expBox = document.getElementById('byjusExplanationBox');
        if (expBox) expBox.style.display = 'none';

        const optionsContainer = document.getElementById('byjusOptionsContainer');
        optionsContainer.innerHTML = '';

        const opts = (showHindi && q.hindiOptions) ? q.hindiOptions : q.options;

        opts.forEach((optText, idx) => {
            const btn = document.createElement('button');
            btn.className = 'byjus-option-btn';
            btn.innerHTML = `
                <span class="byjus-opt-letter">${String.fromCharCode(65 + idx)}</span>
                <span class="byjus-opt-text">${optText}</span>
            `;
            btn.onclick = () => handleSelectOption(idx);
            optionsContainer.appendChild(btn);
        });

        const btnNext = document.getElementById('byjusBtnNext');
        if (btnNext) {
            btnNext.disabled = true;
            btnNext.textContent = (currentNum === total) ? 'Finish Quiz ➔' : 'Next Question ➔';
        }

        const btnSkip = document.getElementById('byjusBtnSkip');
        if (btnSkip) btnSkip.disabled = false;
    }

    function handleSelectOption(selectedIndex) {
        if (activeQuiz.isAnswerLocked) return;
        activeQuiz.isAnswerLocked = true;

        const q = activeQuiz.questions[activeQuiz.currentIndex];
        const isCorrect = (selectedIndex === q.correctIndex);
        const optionsBtns = document.querySelectorAll('.byjus-option-btn');

        if (isCorrect) {
            activeQuiz.score++;
            updateScorePill();
        }

        optionsBtns.forEach((btn, idx) => {
            btn.disabled = true;
            if (idx === q.correctIndex) {
                btn.classList.add('correct');
            } else if (idx === selectedIndex && !isCorrect) {
                btn.classList.add('wrong');
            }
        });

        activeQuiz.reviewItems.push({
            question: q.question,
            hindiQuestion: q.hindiQuestion,
            options: q.options,
            hindiOptions: q.hindiOptions,
            selectedIndex: selectedIndex,
            correctIndex: q.correctIndex,
            isCorrect: isCorrect,
            isSkipped: false,
            explanation: q.explanation,
            hindiExplanation: q.hindiExplanation
        });

        showExplanationBox(q, isCorrect);

        const btnNext = document.getElementById('byjusBtnNext');
        if (btnNext) btnNext.disabled = false;

        const btnSkip = document.getElementById('byjusBtnSkip');
        if (btnSkip) btnSkip.disabled = true;
    }

    function showExplanationBox(q, isCorrect) {
        const expBox = document.getElementById('byjusExplanationBox');
        if (!expBox) return;

        const showHindi = activeQuiz.isHindiMode && q.hindiExplanation;
        const expText = showHindi ? q.hindiExplanation : (q.explanation || 'Refer to standard BYJU\'s Static GK notes for details.');

        expBox.style.display = 'block';
        expBox.innerHTML = `
            <div class="byjus-exp-header ${isCorrect ? 'exp-correct' : 'exp-wrong'}">
                <span>${isCorrect ? '✅ Correct Answer!' : '❌ Incorrect Answer'}</span>
                <span>Correct Option: ${String.fromCharCode(65 + q.correctIndex)}</span>
            </div>
            <div class="byjus-exp-body">
                <strong>Explanation / परीक्षा संदर्भ:</strong> ${expText}
            </div>
        `;
    }

    window.byjusSkipQuestion = function () {
        if (activeQuiz.isAnswerLocked) return;
        activeQuiz.isAnswerLocked = true;

        const q = activeQuiz.questions[activeQuiz.currentIndex];
        const optionsBtns = document.querySelectorAll('.byjus-option-btn');

        optionsBtns.forEach((btn, idx) => {
            btn.disabled = true;
            if (idx === q.correctIndex) {
                btn.classList.add('correct');
            }
        });

        activeQuiz.reviewItems.push({
            question: q.question,
            hindiQuestion: q.hindiQuestion,
            options: q.options,
            hindiOptions: q.hindiOptions,
            selectedIndex: -1,
            correctIndex: q.correctIndex,
            isCorrect: false,
            isSkipped: true,
            explanation: q.explanation,
            hindiExplanation: q.hindiExplanation
        });

        showExplanationBox(q, false);

        const btnNext = document.getElementById('byjusBtnNext');
        if (btnNext) btnNext.disabled = false;

        const btnSkip = document.getElementById('byjusBtnSkip');
        if (btnSkip) btnSkip.disabled = true;
    };

    window.byjusNextQuestion = function () {
        if (activeQuiz.currentIndex + 1 < activeQuiz.questions.length) {
            activeQuiz.currentIndex++;
            renderCurrentQuestion();
        } else {
            finishQuiz();
        }
    };

    window.byjusToggleLanguage = function () {
        activeQuiz.isHindiMode = !activeQuiz.isHindiMode;
        const btn = document.getElementById('byjusBtnLanguage');
        if (btn) {
            btn.textContent = activeQuiz.isHindiMode ? 'A English View' : 'अ Hindi View';
        }

        const q = activeQuiz.questions[activeQuiz.currentIndex];
        const qTextEl = document.getElementById('byjusQuestionText');
        const showHindi = activeQuiz.isHindiMode && q.hindiQuestion;
        qTextEl.textContent = showHindi ? q.hindiQuestion : q.question;

        if (activeQuiz.isAnswerLocked) {
            const isCorrect = activeQuiz.reviewItems[activeQuiz.reviewItems.length - 1]?.isCorrect || false;
            showExplanationBox(q, isCorrect);
        }
    };

    function finishQuiz() {
        if (activeQuiz.timerInterval) clearInterval(activeQuiz.timerInterval);

        const total = activeQuiz.questions.length;
        const score = activeQuiz.score;
        const accuracy = Math.round((score / total) * 100);

        if (activeQuiz.topicId && activeQuiz.topicId !== 'mega_mixed') {
            const key = `byjus_gk_${activeQuiz.topicId}`;
            localStorage.setItem(`${key}_completed`, 'true');
            const prevScore = parseFloat(localStorage.getItem(`${key}_score`) || 0);
            if (score > prevScore || !localStorage.getItem(`${key}_score`)) {
                localStorage.setItem(`${key}_score`, score.toString());
            }
            updateOverallStats();
            renderTopicCards();
        }

        document.getElementById('byjusActiveView').style.display = 'none';
        const resView = document.getElementById('byjusResultsView');
        resView.style.display = 'block';

        document.getElementById('byjusResScoreDigits').textContent = `${score} / ${total}`;
        document.getElementById('byjusResAccuracy').textContent = `${accuracy}% Accuracy`;

        let badgeText = '🏆 Outstanding! Master Level';
        let badgeClass = 'badge-master';
        if (accuracy < 50) {
            badgeText = '📚 Needs Practice & Revision';
            badgeClass = 'badge-needs-work';
        } else if (accuracy < 80) {
            badgeText = '👍 Good Effort! Keep Going';
            badgeClass = 'badge-good';
        }

        const badgeEl = document.getElementById('byjusResBadge');
        badgeEl.textContent = badgeText;
        badgeEl.className = `byjus-results-badge ${badgeClass}`;

        const skipped = activeQuiz.reviewItems.filter(i => i.isSkipped).length;
        const wrong = total - score - skipped;

        document.getElementById('byjusResCorrect').textContent = score;
        document.getElementById('byjusResWrong').textContent = wrong;
        document.getElementById('byjusResSkipped').textContent = skipped;

        const m = Math.floor(activeQuiz.timerSeconds / 60);
        const s = activeQuiz.timerSeconds % 60;
        document.getElementById('byjusResTime').textContent = `${m}m ${s}s`;

        renderReviewList();
    }

    function renderReviewList() {
        const list = document.getElementById('byjusReviewList');
        if (!list) return;

        let html = '';
        activeQuiz.reviewItems.forEach((item, idx) => {
            const showHindi = activeQuiz.isHindiMode && item.hindiQuestion;
            const qText = showHindi ? item.hindiQuestion : item.question;
            const opts = (showHindi && item.hindiOptions) ? item.hindiOptions : item.options;
            const exp = (showHindi && item.hindiExplanation) ? item.hindiExplanation : item.explanation;

            let statusIcon = '✅';
            let statusClass = 'review-correct';
            let userAnsText = opts[item.selectedIndex] || 'None';

            if (item.isSkipped) {
                statusIcon = '⚠️ Skipped';
                statusClass = 'review-skipped';
                userAnsText = 'Skipped by user';
            } else if (!item.isCorrect) {
                statusIcon = '❌ Wrong';
                statusClass = 'review-wrong';
            }

            const correctAnsText = opts[item.correctIndex];

            html += `
                <div class="byjus-review-item ${statusClass}">
                    <div class="byjus-review-header">
                        <span class="byjus-review-num">Q${idx + 1}</span>
                        <span class="byjus-review-status">${statusIcon}</span>
                    </div>
                    <div class="byjus-review-question">${qText}</div>
                    <div class="byjus-review-answers">
                        <div class="byjus-ans-row">
                            <span class="byjus-ans-lbl">Your Answer:</span>
                            <span class="byjus-ans-val ${item.isCorrect ? 'text-correct' : 'text-wrong'}">${userAnsText}</span>
                        </div>
                        <div class="byjus-ans-row">
                            <span class="byjus-ans-lbl">Correct Answer:</span>
                            <span class="byjus-ans-val text-correct">${correctAnsText}</span>
                        </div>
                    </div>
                    <div class="byjus-review-exp">
                        <strong>Fact Reference:</strong> ${exp || 'Based on BYJU\'s Static GK database.'}
                    </div>
                </div>
            `;
        });

        list.innerHTML = html;
    }

    window.byjusRetestCurrentTopic = function () {
        if (activeQuiz.topicId === 'mega_mixed') {
            window.startMegaMixedQuiz();
        } else {
            window.startByjusQuiz(activeQuiz.topicId, 'all');
        }
    };

    window.byjusCloseModal = function () {
        const modal = document.getElementById('byjusQuizModal');
        if (modal) modal.classList.remove('active');
        document.body.style.overflow = '';
        if (activeQuiz.timerInterval) clearInterval(activeQuiz.timerInterval);
    };

    window.byjusConfirmExit = function () {
        if (!activeQuiz.isOpen) {
            window.byjusCloseModal();
            return;
        }

        const confirmModal = document.getElementById('byjusExitConfirmModal');
        if (confirmModal) confirmModal.classList.add('active');
    };

    window.byjusStayInTest = function () {
        const confirmModal = document.getElementById('byjusExitConfirmModal');
        if (confirmModal) confirmModal.classList.remove('active');
    };

    window.byjusExecuteExit = function () {
        const confirmModal = document.getElementById('byjusExitConfirmModal');
        if (confirmModal) confirmModal.classList.remove('active');
        window.byjusCloseModal();
    };

    // Initialize on load
    document.addEventListener('DOMContentLoaded', init);

})();
