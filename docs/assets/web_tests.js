/**
 * QUIZEE WEB TESTS PORTAL & LEVEL-WISE 100-QUESTION MOCK ENGINE
 * Clean, High-Performance, Real Exam Simulator
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

    const MOCK_LEVEL_CONFIG = [
        {
            level: 1,
            name: "Level 1: Foundation",
            badge: "Easy",
            pillClass: "easy",
            desc: "Direct foundational MCQs • 15 Mocks",
            mockCount: 15
        },
        {
            level: 2,
            name: "Level 2: Standard CGL",
            badge: "Moderate",
            pillClass: "moderate",
            desc: "Tier-1 standard shift level • 15 Mocks",
            mockCount: 15
        },
        {
            level: 3,
            name: "Level 3: Advanced",
            badge: "Hard",
            pillClass: "hard",
            desc: "In-depth & tricky questions • 15 Mocks",
            mockCount: 15
        },
        {
            level: 4,
            name: "Level 4: Master Pro",
            badge: "Pro Ranker",
            pillClass: "pro",
            desc: "Top percentile challenge • 15 Mocks",
            mockCount: 15
        }
    ];

    const MOCK_SECTIONS_SPEC = [
        { key: 'english', name: 'English Language', icon: '📖', count: 30 },
        { key: 'static_gk', name: 'Static GK', icon: '🏛️', count: 18 },
        { key: 'history', name: 'Indian History', icon: '🏺', count: 14 },
        { key: 'polity', name: 'Indian Polity', icon: '⚖️', count: 12 },
        { key: 'geography', name: 'Geography', icon: '🌍', count: 10 },
        { key: 'biology', name: 'Science & Biology', icon: '🧬', count: 10 },
        { key: 'economics', name: 'Indian Economy', icon: '📈', count: 6 }
    ];

    // Global State
    let QUIZEE_CURRICULUM = [];
    let portalState = {
        activeTab: 'topics', // 'mock' or 'topics'
        activeSubject: 'English',
        searchQuery: '',
        selectedTopics: new Set(),
        expandedTopics: new Set(),
        selectedMultiCount: 50
    };

    let mockPortalState = {
        activeLevel: 2, // Default to Standard CGL
        selectedMockNum: 1,
        isExamMode: true // 60 Min Timer, +2 / -0.50 marks
    };

    let activeTest = {
        isOpen: false,
        title: '',
        subtitle: '',
        setKey: null,
        is100QMock: false,
        isExamMode: false,
        mockLevel: 2,
        mockNum: 1,
        questions: [],
        currentIndex: 0,
        score: 0,
        isAnswerLocked: false,
        isHindiMode: false,
        reviewItems: [],
        userAnswers: {}, // index -> chosenOptionIndex
        markedForReview: new Set(),
        timerSeconds: 0,
        remainingSeconds: 3600,
        timerInterval: null
    };

    const translationCache = {};

    function cleanCategoryName(catName, subjectName) {
        if (!catName) return subjectName;
        if (catName.startsWith('English:')) return catName.replace('English:', '').trim();
        if (catName.startsWith('Modern History:')) return catName.replace('Modern History:', '').trim() + ' (Modern)';
        if (catName.startsWith('Medieval History:')) return catName.replace('Medieval History:', '').trim() + ' (Medieval)';
        if (catName.startsWith('Ancient History:')) return catName.replace('Ancient History:', '').trim() + ' (Ancient)';
        if (catName.startsWith('Polity:')) return catName.replace('Polity:', '').trim();
        if (catName.startsWith('General Science:')) return catName.replace('General Science:', '').trim();
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
                topics
            });
        });

        return curriculum;
    }

    // =========================================================
    // LEVEL-WISE 100-QUESTION MOCK TEST GENERATOR
    // =========================================================

    function generateLevelMockTest(levelNumber, mockNumber) {
        const questionsData = window.QUIZEE_ALL_QUESTIONS || {};
        const mockQuestions = [];

        MOCK_SECTIONS_SPEC.forEach(spec => {
            const pool = questionsData[spec.key] || [];
            if (pool.length === 0) return;

            const n = pool.length;
            let subPool = [];

            // Level Slice
            if (levelNumber === 1) {
                subPool = pool.slice(0, Math.max(spec.count, Math.floor(n * 0.25)));
            } else if (levelNumber === 2) {
                subPool = pool.slice(Math.floor(n * 0.25), Math.max(Math.floor(n * 0.25) + spec.count, Math.floor(n * 0.55)));
            } else if (levelNumber === 3) {
                subPool = pool.slice(Math.floor(n * 0.55), Math.max(Math.floor(n * 0.55) + spec.count, Math.floor(n * 0.80)));
            } else {
                subPool = pool.slice(Math.floor(n * 0.80));
                if (subPool.length < spec.count) subPool = pool.slice(-spec.count);
            }

            if (subPool.length < spec.count) subPool = pool;

            // Circular wrapping across 15+ mocks
            const nSub = subPool.length;
            const offset = ((mockNumber - 1) * spec.count) % nSub;
            const selected = [];
            for (let i = 0; i < spec.count; i++) {
                selected.push(subPool[(offset + i) % nSub]);
            }

            selected.forEach(q => {
                mockQuestions.push({
                    ...q,
                    mockSection: spec.name,
                    mockSectionIcon: spec.icon,
                    mockSectionKey: spec.key
                });
            });
        });

        return mockQuestions;
    }

    // =========================================================
    // RENDER FUNCTIONS
    // =========================================================

    function renderPortalModeNav() {
        let container = document.getElementById('portalModeNav');
        if (!container) {
            const portalHeader = document.querySelector('.portal-header-box');
            if (portalHeader) {
                container = document.createElement('div');
                container.id = 'portalModeNav';
                container.className = 'portal-mode-nav';
                portalHeader.parentNode.insertBefore(container, portalHeader.nextSibling);
            }
        }
        if (!container) return;

        container.innerHTML = `
            <button class="portal-mode-btn ${portalState.activeTab === 'mock' ? 'active' : ''}" onclick="window.switchPortalTab('mock')">
                <span>🎯 Full Mock Tests (100 Qs)</span>
                <span class="mode-badge">Level 1 - 4</span>
            </button>
            <button class="portal-mode-btn ${portalState.activeTab === 'topics' ? 'active' : ''}" onclick="window.switchPortalTab('topics')">
                <span>📚 Topic-Wise Practice Sets</span>
                <span style="font-size:11px; color:#94a3b8;">7 Subjects</span>
            </button>
        `;
    }

    function renderMockHub() {
        let container = document.getElementById('mockHubContainer');
        if (!container) {
            const modeNav = document.getElementById('portalModeNav');
            if (modeNav) {
                container = document.createElement('div');
                container.id = 'mockHubContainer';
                container.className = 'mock-hub-container';
                modeNav.parentNode.insertBefore(container, modeNav.nextSibling);
            }
        }
        if (!container) return;

        if (portalState.activeTab !== 'mock') {
            container.style.display = 'none';
            return;
        }
        container.style.display = 'block';

        const activeLevelObj = MOCK_LEVEL_CONFIG.find(l => l.level === mockPortalState.activeLevel) || MOCK_LEVEL_CONFIG[1];

        // Level cards HTML
        const levelCardsHtml = MOCK_LEVEL_CONFIG.map(l => `
            <div class="mock-level-card ${l.level === mockPortalState.activeLevel ? 'active' : ''}" onclick="window.selectMockLevel(${l.level})">
                <div class="level-badge-row">
                    <span class="level-pill ${l.pillClass}">${l.badge}</span>
                    <span style="font-size:11px; color:#94a3b8; font-weight:700;">100 Qs</span>
                </div>
                <div class="mock-level-name">${l.name}</div>
                <div class="mock-level-desc">${l.desc}</div>
            </div>
        `).join('');

        // Mock test items HTML (Mock 1 to 5)
        const mockItemsHtml = [1, 2, 3, 4, 5].map(mNum => {
            const setKey = `mock_level_${mockPortalState.activeLevel}_test_${mNum}`;
            const completed = localStorage.getItem(`${setKey}_completed`) === 'true';
            const score = localStorage.getItem(`${setKey}_score`);

            let scoreBadge = 'Unattempted';
            if (completed && score !== null) {
                const marks = parseFloat(score);
                scoreBadge = `Score: ${marks.toFixed(1)} / 200`;
            }

            const isSelected = mNum === mockPortalState.selectedMockNum;

            return `
                <div class="mock-test-item ${isSelected ? 'selected' : ''}" onclick="window.selectMockNumber(${mNum})">
                    <div class="mock-item-top">
                        <span class="mock-item-number">Mock Test #${mNum}</span>
                        <span class="mock-item-score-badge">${scoreBadge}</span>
                    </div>
                    <div class="mock-item-meta">
                        100 Questions • 60 Mins • 200 Marks<br>
                        <span style="color:#6366f1;">All 7 Subjects Balanced</span>
                    </div>
                    <button class="btn-select-mock">
                        ${isSelected ? 'Selected ✓' : 'Select Test'}
                    </button>
                </div>
            `;
        }).join('');

        container.innerHTML = `
            <div class="mock-hub-header">
                <div class="mock-hub-title-box">
                    <h3>🎯 SSC CGL Full-Length 100-Question Mock Tests</h3>
                    <p>
                        Simulate real Tier-1 examination conditions with exact multi-subject syllabus coverage: 
                        <b>30 English</b>, <b>18 Static GK</b>, <b>14 History</b>, <b>12 Polity</b>, <b>10 Geography</b>, <b>10 Science</b>, and <b>6 Economics</b>.
                    </p>
                </div>
                <div style="font-size:12px; font-weight:700; color:#10b981; background:rgba(16,185,129,0.12); border:1px solid rgba(16,185,129,0.3); padding:6px 14px; border-radius:20px;">
                    ⚡ 7,480 Verified Questions Pool
                </div>
            </div>

            <!-- Level Selector -->
            <div class="mock-level-tabs">
                ${levelCardsHtml}
            </div>

            <!-- Mock Tests Grid -->
            <div class="mock-tests-grid">
                ${mockItemsHtml}
            </div>

            <!-- Controls and Start -->
            <div class="mock-controls-bar">
                <div class="mock-mode-options">
                    <span class="mock-mode-label">Mode:</span>
                    <button class="mock-toggle-btn ${mockPortalState.isExamMode ? 'active' : ''}" onclick="window.setMockMode(true)">
                        ⏱️ Real Exam Mode (60 Mins, +2 / -0.50)
                    </button>
                    <button class="mock-toggle-btn ${!mockPortalState.isExamMode ? 'active' : ''}" onclick="window.setMockMode(false)">
                        ⚡ Practice Mode (Untimed, Instant Feedback)
                    </button>
                </div>

                <button class="btn-launch-mock-test" onclick="window.startSelectedMockTest()">
                    <span>🚀 Launch Mock Test #${mockPortalState.selectedMockNum} (100 Qs)</span>
                </button>
            </div>
        `;
    }

    function renderSubjectTabs() {
        const tabsWrap = document.getElementById('portalSubjectTabs');
        if (!tabsWrap) return;

        tabsWrap.innerHTML = '';
        QUIZEE_CURRICULUM.forEach(sub => {
            const btn = document.createElement('button');
            btn.className = `portal-tab-pill ${sub.name === portalState.activeSubject ? 'active' : ''}`;
            btn.innerHTML = `<span>${sub.icon}</span> <span>${sub.name}</span> <span style="font-size:11px; opacity:0.8;">(${sub.totalQuestions})</span>`;
            btn.onclick = () => {
                portalState.activeSubject = sub.name;
                renderSubjectTabs();
                renderTopicsGrid();
            };
            tabsWrap.appendChild(btn);
        });
    }

    function renderLastPracticedBanner() {
        const banner = document.getElementById('portalResumeBanner');
        if (!banner) return;

        const lastKey = localStorage.getItem('last_practiced_set_key');
        if (!lastKey) {
            banner.style.display = 'none';
            return;
        }

        const title = localStorage.getItem('last_practiced_title') || 'Recent Test';
        const subtitle = localStorage.getItem('last_practiced_subtitle') || '';
        const score = localStorage.getItem('last_practiced_score') || '0';
        const total = localStorage.getItem('last_practiced_total') || '100';

        banner.style.display = 'flex';
        banner.innerHTML = `
            <div class="resume-info">
                <span class="resume-tag">⚡ Jump Back In</span>
                <span class="resume-title">${title} (${score}/${total})</span>
                <span class="resume-sub">${subtitle}</span>
            </div>
            <button class="btn-resume-set" onclick="window.resumeLastSet('${lastKey}')">Resume Practice ➔</button>
        `;
    }

    function renderTopicsGrid() {
        const grid = document.getElementById('portalTopicsGrid');
        const tabsWrap = document.querySelector('.portal-tabs-wrap');
        const controlsBar = document.querySelector('.portal-controls-bar');
        const resumeBanner = document.getElementById('portalResumeBanner');

        if (portalState.activeTab === 'mock') {
            if (grid) grid.style.display = 'none';
            if (tabsWrap) tabsWrap.style.display = 'none';
            if (controlsBar) controlsBar.style.display = 'none';
            if (resumeBanner) resumeBanner.style.display = 'none';
            return;
        }

        if (grid) grid.style.display = 'grid';
        if (tabsWrap) tabsWrap.style.display = 'block';
        if (controlsBar) controlsBar.style.display = 'flex';
        if (resumeBanner && localStorage.getItem('last_practiced_set_key')) resumeBanner.style.display = 'flex';

        if (!grid) return;
        const currentSub = QUIZEE_CURRICULUM.find(s => s.name === portalState.activeSubject);
        if (!currentSub) return;

        let topics = currentSub.topics;
        if (portalState.searchQuery.trim()) {
            const q = portalState.searchQuery.toLowerCase();
            topics = topics.filter(t => t.displayName.toLowerCase().includes(q) || t.categoryName.toLowerCase().includes(q));
        }

        if (topics.length === 0) {
            grid.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding: 40px; color:#94a3b8;">No topics found matching "${portalState.searchQuery}".</div>`;
            return;
        }

        grid.innerHTML = topics.map(t => {
            const isSelected = portalState.selectedTopics.has(t.topicId);
            const isExpanded = portalState.expandedTopics.has(t.topicId);

            const setsHtml = t.sets.map(s => `
                <div class="topic-set-card ${s.isCompleted ? 'completed' : ''}" onclick="window.startSpecificSet('${s.setKey}')">
                    <div class="set-card-header">
                        <span class="set-number-tag">Set ${s.setNumber}</span>
                        <span class="set-status-badge">${s.statusBadge || '100 Qs'}</span>
                    </div>
                    <div class="set-range-label">Questions ${s.startRange} - ${s.endRange}</div>
                    <button class="btn-play-set">${s.isCompleted ? 'Re-test ↺' : 'Start Set ➔'}</button>
                </div>
            `).join('');

            return `
                <div class="topic-item-card ${isSelected ? 'selected' : ''}" id="${t.topicId}">
                    <div class="topic-card-header" onclick="window.toggleTopicSets('${t.topicId}')">
                        <div class="topic-header-left">
                            <input type="checkbox" class="topic-checkbox" ${isSelected ? 'checked' : ''} onclick="event.stopPropagation(); window.toggleSelectTopic('${t.topicId}')">
                            <div>
                                <div class="topic-title-text">${t.displayName}</div>
                                <div class="topic-meta-text">${t.totalQuestions} Questions • ${t.sets.length} Sets</div>
                            </div>
                        </div>
                        <div class="topic-header-right">
                            <span class="topic-progress-badge">${t.completedSets}/${t.sets.length} Sets</span>
                            <span class="topic-expand-arrow ${isExpanded ? 'open' : ''}">▼</span>
                        </div>
                    </div>
                    <div class="topic-sets-tray ${isExpanded ? 'open' : ''}">
                        <div class="sets-grid-wrap">
                            ${setsHtml}
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        updateMultiDockUI();
    }

    function updateMultiDockUI() {
        const dock = document.getElementById('portalMultiDock');
        if (!dock) return;

        if (portalState.selectedTopics.size > 0 && portalState.activeTab === 'topics') {
            dock.classList.add('active');
            let totalQs = 0;
            portalState.selectedTopics.forEach(tId => {
                const topic = findTopicById(tId);
                if (topic) totalQs += topic.totalQuestions;
            });
            const countText = document.getElementById('dockSelectedCountText');
            if (countText) countText.textContent = `Selected: ${portalState.selectedTopics.size} Topics (Pool: ${totalQs} Qs)`;
            const btn = document.getElementById('btnStartMultiMixed');
            if (btn) btn.textContent = `🚀 Start Custom Mixed Test (${portalState.selectedMultiCount} Qs)`;
        } else {
            dock.classList.remove('active');
        }
    }

    function findTopicById(topicId) {
        for (const sub of QUIZEE_CURRICULUM) {
            for (const top of sub.topics) {
                if (top.topicId === topicId) return top;
            }
        }
        return null;
    }

    // =========================================================
    // WINDOW GLOBAL EVENT HANDLERS
    // =========================================================

    window.switchPortalTab = function (tabName) {
        portalState.activeTab = tabName;
        renderPortalModeNav();
        renderMockHub();
        renderTopicsGrid();
    };

    window.selectMockLevel = function (level) {
        mockPortalState.activeLevel = level;
        renderMockHub();
    };

    window.selectMockNumber = function (mockNum) {
        mockPortalState.selectedMockNum = mockNum;
        renderMockHub();
    };

    window.setMockMode = function (isExam) {
        mockPortalState.isExamMode = isExam;
        renderMockHub();
    };

    window.startMockDirectly = async function (level, mockNum, isExam) {
        if (!window.QUIZEE_ALL_QUESTIONS) {
            await loadAllQuestions();
        }
        const levelObj = MOCK_LEVEL_CONFIG.find(l => l.level === level) || MOCK_LEVEL_CONFIG[1];
        const questions = generateLevelMockTest(level, mockNum);

        if (!questions || questions.length === 0) {
            alert('Unable to load mock test questions. Please check connection.');
            return;
        }

        const title = `Mock Test #${mockNum < 10 ? '0' + mockNum : mockNum} (${levelObj.name})`;
        const subtitle = `100 Questions • 60 Mins • 200 Marks (+2.0 / -0.50)`;
        const setKey = `mock_level_${level}_test_${mockNum}`;

        launchWebTest(questions, title, subtitle, setKey, true, isExam, level, mockNum);
    };

    window.startSelectedMockTest = function () {
        const level = mockPortalState.activeLevel;
        const mockNum = mockPortalState.selectedMockNum;
        const isExam = mockPortalState.isExamMode;

        const levelObj = MOCK_LEVEL_CONFIG.find(l => l.level === level) || MOCK_LEVEL_CONFIG[1];
        const questions = generateLevelMockTest(level, mockNum);

        if (!questions || questions.length === 0) {
            alert('Unable to load mock test questions. Please check connection.');
            return;
        }

        const title = `SSC CGL Mock Test #${mockNum} (${levelObj.name})`;
        const subtitle = `${levelObj.desc} • 100 Questions • 200 Marks • 60 Mins`;
        const setKey = `mock_level_${level}_test_${mockNum}`;

        launchWebTest(questions, title, subtitle, setKey, true, isExam, level, mockNum);
    };

    window.toggleTopicSets = function (topicId) {
        if (portalState.expandedTopics.has(topicId)) {
            portalState.expandedTopics.delete(topicId);
        } else {
            portalState.expandedTopics.add(topicId);
        }
        renderTopicsGrid();
    };

    window.toggleSelectTopic = function (topicId) {
        if (portalState.selectedTopics.has(topicId)) {
            portalState.selectedTopics.delete(topicId);
        } else {
            portalState.selectedTopics.add(topicId);
        }
        renderTopicsGrid();
    };

    window.clearTopicSelections = function () {
        portalState.selectedTopics.clear();
        renderTopicsGrid();
    };

    window.selectMultiCount = function (count) {
        portalState.selectedMultiCount = count;
        document.querySelectorAll('.dock-chip').forEach(c => {
            c.classList.toggle('active', c.textContent.includes(`${count}`));
        });
        updateMultiDockUI();
    };

    window.startSpecificSet = function (setKey) {
        for (const sub of QUIZEE_CURRICULUM) {
            for (const top of sub.topics) {
                for (const set of top.sets) {
                    if (set.setKey === setKey) {
                        launchWebTest(set.questions, `${top.displayName} - Set ${set.setNumber}`, `${sub.name} Practice`, setKey, false, false);
                        return;
                    }
                }
            }
        }
    };

    window.resumeLastSet = function (setKey) {
        window.startSpecificSet(setKey);
    };

    window.startCustomMixedTest = function () {
        if (portalState.selectedTopics.size === 0) return;
        const allPool = [];
        portalState.selectedTopics.forEach(tId => {
            const topic = findTopicById(tId);
            if (topic) allPool.push(...topic.questions);
        });
        if (allPool.length === 0) return;
        const shuffled = allPool.sort(() => 0.5 - Math.random());
        const targetCount = Math.min(portalState.selectedMultiCount, shuffled.length);
        const selected = shuffled.slice(0, targetCount);

        launchWebTest(selected, `Custom Multi-Topic Test (${targetCount} Qs)`, `Mixed Practice from ${portalState.selectedTopics.size} Topics`, 'custom_multi', false, false);
    };

    // =========================================================
    // WEB TEST PLAYER
    // =========================================================

    function launchWebTest(questions, title, subtitle, setKey, is100QMock = false, isExamMode = false, mockLevel = 2, mockNum = 1) {
        if (!questions || questions.length === 0) return;

        activeTest.isOpen = true;
        activeTest.title = title;
        activeTest.subtitle = subtitle;
        activeTest.setKey = setKey;
        activeTest.is100QMock = is100QMock;
        activeTest.isExamMode = isExamMode;
        activeTest.mockLevel = mockLevel;
        activeTest.mockNum = mockNum;
        activeTest.questions = questions;
        activeTest.currentIndex = 0;
        activeTest.score = 0;
        activeTest.isAnswerLocked = false;
        activeTest.isHindiMode = false;
        activeTest.reviewItems = [];
        activeTest.userAnswers = {};
        activeTest.markedForReview = new Set();
        activeTest.timerSeconds = 0;
        activeTest.remainingSeconds = 3600; // 60 mins for mock exam

        clearInterval(activeTest.timerInterval);
        activeTest.timerInterval = setInterval(() => {
            activeTest.timerSeconds++;
            if (activeTest.isExamMode) {
                activeTest.remainingSeconds--;
                updateTimerDisplay();
                if (activeTest.remainingSeconds <= 0) {
                    clearInterval(activeTest.timerInterval);
                    alert("⏰ Time's up! Submitting your 60-minute Mock Exam now.");
                    showWebTestResults();
                }
            } else {
                updateTimerDisplay();
            }
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

        // Add 100-Q Palette Button & Timer Pill if not present
        ensureHeaderTools();

        // Render Section Jump Bar if 100-Q Mock Test
        renderSectionJumpBar();

        displayActiveQuestion(0);
    }

    function ensureHeaderTools() {
        const headerRight = document.querySelector('.test-header-right');
        if (!headerRight) return;

        let timerPill = document.getElementById('webTestTimerPill');
        if (!timerPill) {
            timerPill = document.createElement('span');
            timerPill.id = 'webTestTimerPill';
            timerPill.style.cssText = 'font-size:12px; font-weight:800; color:#94a3b8; background:rgba(255,255,255,0.06); border:1px solid rgba(255,255,255,0.1); padding:4px 10px; border-radius:12px; display:inline-flex; align-items:center;';
            timerPill.textContent = '⏱️ 00:00';
            headerRight.insertBefore(timerPill, headerRight.firstChild);
        }

        let paletteBtn = document.getElementById('btnOpenPalette');
        if (!paletteBtn) {
            paletteBtn = document.createElement('button');
            paletteBtn.id = 'btnOpenPalette';
            paletteBtn.style.cssText = 'font-size:12px; font-weight:700; color:#818cf8; background:rgba(99,102,241,0.15); border:1px solid rgba(99,102,241,0.3); padding:4px 10px; border-radius:8px; cursor:pointer;';
            paletteBtn.textContent = '📊 Palette';
            paletteBtn.onclick = () => window.openPaletteModal();
            headerRight.insertBefore(paletteBtn, headerRight.firstChild);
        }
    }

    function updateTimerDisplay() {
        const timerPill = document.getElementById('webTestTimerPill');
        if (!timerPill) return;

        if (activeTest.isExamMode) {
            const rem = Math.max(0, activeTest.remainingSeconds);
            const m = Math.floor(rem / 60);
            const s = rem % 60;
            const timeStr = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
            timerPill.textContent = `⏱️ ${timeStr}`;
            if (rem <= 300) {
                timerPill.style.color = '#ef4444';
                timerPill.style.borderColor = '#ef4444';
            } else {
                timerPill.style.color = '#10b981';
                timerPill.style.borderColor = 'rgba(16,185,129,0.3)';
            }
        } else {
            const m = Math.floor(activeTest.timerSeconds / 60);
            const s = activeTest.timerSeconds % 60;
            timerPill.textContent = `⏱️ ${m}:${s.toString().padStart(2, '0')}`;
            timerPill.style.color = '#94a3b8';
        }
    }

    function renderSectionJumpBar() {
        let jumpBar = document.getElementById('mockSectionJumpBar');
        if (!jumpBar) {
            const header = document.querySelector('.web-test-header');
            if (header) {
                jumpBar = document.createElement('div');
                jumpBar.id = 'mockSectionJumpBar';
                jumpBar.className = 'mock-section-jump-bar';
                header.parentNode.insertBefore(jumpBar, header.nextSibling);
            }
        }
        if (!jumpBar) return;

        if (!activeTest.is100QMock) {
            jumpBar.style.display = 'none';
            return;
        }

        jumpBar.style.display = 'flex';
        jumpBar.innerHTML = '';

        let offset = 0;
        MOCK_SECTIONS_SPEC.forEach((spec) => {
            const startIdx = offset;
            const endIdx = offset + spec.count - 1;
            const btn = document.createElement('button');
            btn.className = `section-jump-pill ${activeTest.currentIndex >= startIdx && activeTest.currentIndex <= endIdx ? 'active' : ''}`;
            btn.innerHTML = `<span>${spec.icon}</span> <span>${spec.name}</span> <span style="opacity:0.75; font-size:10.5px;">(${spec.count})</span>`;
            btn.onclick = () => {
                displayActiveQuestion(startIdx);
            };
            jumpBar.appendChild(btn);
            offset += spec.count;
        });
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
        if (catTag) {
            if (q.mockSection) {
                catTag.innerHTML = `<b>${q.mockSectionIcon || '📌'} ${q.mockSection}</b> • ${q.category || 'SSC CGL'}`;
            } else {
                catTag.textContent = q.category || 'SSC Exam';
            }
        }

        renderQuestionTextAndOptions(q);

        // Update Section jump pills active state
        renderSectionJumpBar();

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
        const textEl = document.getElementById('webQuestionText');
        const optionsContainer = document.getElementById('webOptionsContainer');
        if (!textEl || !optionsContainer) return;

        let displayQ = q.question;
        let displayOpts = [...q.options];

        if (activeTest.isHindiMode) {
            const embedded = extractEmbeddedHindiQuestion(q);
            if (embedded) {
                displayQ = embedded.question;
                displayOpts = embedded.options;
            }
        }

        textEl.textContent = displayQ;
        optionsContainer.innerHTML = '';

        displayOpts.forEach((optText, optIdx) => {
            const btn = document.createElement('button');
            btn.className = 'web-option-btn';
            btn.innerHTML = `
                <span class="option-badge">${String.fromCharCode(65 + optIdx)}</span>
                <span class="option-label">${optText}</span>
            `;
            btn.onclick = () => handleOptionSelected(optIdx);
            optionsContainer.appendChild(btn);
        });
    }

    function extractEmbeddedHindiQuestion(q) {
        const qParts = q.question.split('/');
        if (qParts.length > 1) {
            const hQ = qParts[qParts.length - 1].trim();
            const hOpts = q.options.map(opt => {
                const oParts = opt.split('/');
                return (oParts.length > 1) ? oParts[oParts.length - 1].trim() : opt;
            });
            return { question: hQ, options: hOpts };
        }
        return null;
    }

    function handleOptionSelected(selectedIndex) {
        if (activeTest.isAnswerLocked) return;
        activeTest.isAnswerLocked = true;

        const q = activeTest.questions[activeTest.currentIndex];
        const isCorrect = (selectedIndex === q.correctIndex);
        activeTest.userAnswers[activeTest.currentIndex] = selectedIndex;

        if (isCorrect) activeTest.score++;

        const optButtons = document.querySelectorAll('.web-option-btn');
        optButtons.forEach((btn, idx) => {
            btn.disabled = true;
            if (idx === q.correctIndex) {
                btn.classList.add('correct');
            } else if (idx === selectedIndex) {
                btn.classList.add('wrong');
            }
        });

        activeTest.reviewItems.push({
            questionNumber: activeTest.currentIndex + 1,
            questionText: q.question,
            section: q.mockSection || q.category,
            category: q.category,
            options: q.options,
            selectedIndex: selectedIndex,
            correctIndex: q.correctIndex,
            isCorrect: isCorrect,
            isSkipped: false
        });

        const scorePill = document.getElementById('webTestScorePill');
        if (scorePill) scorePill.textContent = `Score: ${activeTest.score}`;

        const skipBtn = document.getElementById('btnTestSkip');
        if (skipBtn) skipBtn.style.display = 'none';

        const nextBtn = document.getElementById('btnTestNext');
        if (nextBtn) {
            nextBtn.disabled = false;
            nextBtn.focus();
        }

        // In Exam Mode with 100 questions, automatically advance after short delay
        if (activeTest.is100QMock && activeTest.isExamMode) {
            setTimeout(() => {
                handleNextQuestion();
            }, 600);
        }
    }

    function handleSkipQuestion() {
        if (activeTest.isAnswerLocked) return;
        const q = activeTest.questions[activeTest.currentIndex];

        activeTest.reviewItems.push({
            questionNumber: activeTest.currentIndex + 1,
            questionText: q.question,
            section: q.mockSection || q.category,
            category: q.category,
            options: q.options,
            selectedIndex: -1,
            correctIndex: q.correctIndex,
            isCorrect: false,
            isSkipped: true
        });

        handleNextQuestion();
    }

    function handleNextQuestion() {
        if (activeTest.currentIndex < activeTest.questions.length - 1) {
            displayActiveQuestion(activeTest.currentIndex + 1);
        } else {
            showWebTestResults();
        }
    }

    // =========================================================
    // RESULTS & COMPREHENSIVE SCORECARD
    // =========================================================

    function showWebTestResults() {
        clearInterval(activeTest.timerInterval);

        document.getElementById('webTestActiveBody').style.display = 'none';
        document.getElementById('webTestFooter').style.display = 'none';
        document.getElementById('webTestResultsView').style.display = 'block';

        const jumpBar = document.getElementById('mockSectionJumpBar');
        if (jumpBar) jumpBar.style.display = 'none';

        const total = activeTest.questions.length;
        const correct = activeTest.score;
        const wrong = activeTest.reviewItems.filter(r => !r.isCorrect && !r.isSkipped).length;
        const skipped = total - (correct + wrong);
        const accuracy = total > 0 ? Math.round((correct / total) * 100) : 0;

        // Save progress to local storage
        if (activeTest.setKey && activeTest.setKey !== 'custom_multi') {
            const prevScore = parseInt(localStorage.getItem(`${activeTest.setKey}_score`) || '0', 10);
            if (correct > prevScore || !localStorage.getItem(`${activeTest.setKey}_completed`)) {
                localStorage.setItem(`${activeTest.setKey}_completed`, 'true');
                localStorage.setItem(`${activeTest.setKey}_score`, `${correct}`);
                localStorage.setItem(`${activeTest.setKey}_total`, `${total}`);
            }
            localStorage.setItem('last_practiced_set_key', activeTest.setKey);
            localStorage.setItem('last_practiced_title', activeTest.title);
            localStorage.setItem('last_practiced_subtitle', activeTest.subtitle);
            localStorage.setItem('last_practiced_score', `${correct}`);
            localStorage.setItem('last_practiced_total', `${total}`);
        }

        // If 100-Q Mock Test: Real SSC CGL Marking (+2 / -0.50 out of 200)
        if (activeTest.is100QMock) {
            const positiveMarks = correct * 2.0;
            const negativeMarks = wrong * 0.50;
            const netMarks = Math.max(0, positiveMarks - negativeMarks);

            document.getElementById('resultsMainTitle').textContent = `Score: ${netMarks.toFixed(1)} / 200.0 Marks`;
            document.getElementById('resultsSubtitle').textContent = `${activeTest.title} Completed • ${total} Questions`;

            let badgeCallout = '';
            if (netMarks >= 150) badgeCallout = '🏆 Outstanding! Expected All-India Rank < 500';
            else if (netMarks >= 135) badgeCallout = '🎯 Tier-1 Qualified! Excellent Performance';
            else if (netMarks >= 110) badgeCallout = '⚡ Good Score! Clear weak sections to boost ranking';
            else badgeCallout = '📚 Needs Revision. Practice weak topics and review solutions';

            document.getElementById('resultsBadgeCallout').textContent = badgeCallout;
            document.getElementById('resultsScoreDigits').textContent = `${correct} / ${total}`;
            document.getElementById('resultsAccuracyPill').textContent = `${accuracy}% Net Accuracy • Net Marks: ${netMarks.toFixed(1)}`;

            // Render Section Breakdown Table
            renderMockSectionalTable();
        } else {
            document.getElementById('resultsMainTitle').textContent = 'Set Completed!';
            document.getElementById('resultsSubtitle').textContent = activeTest.title;
            document.getElementById('resultsScoreDigits').textContent = `${correct} / ${total}`;
            document.getElementById('resultsAccuracyPill').textContent = `${accuracy}% Accuracy`;
            document.getElementById('resultsBadgeCallout').textContent = (accuracy >= 80) ? '🏆 Master Level' : (accuracy >= 50 ? '🎯 Good Job' : '⚠️ Keep Practicing');
        }

        document.getElementById('resultsMetricCorrect').textContent = `${correct}`;
        document.getElementById('resultsMetricWrong').textContent = `${wrong}`;
        document.getElementById('resultsMetricSkipped').textContent = `${skipped}`;

        renderReviewList();
    }

    function renderMockSectionalTable() {
        let tableContainer = document.getElementById('mockSectionalTableContainer');
        if (!tableContainer) {
            const scoreCard = document.querySelector('.results-score-card');
            if (scoreCard) {
                tableContainer = document.createElement('div');
                tableContainer.id = 'mockSectionalTableContainer';
                scoreCard.appendChild(tableContainer);
            }
        }
        if (!tableContainer) return;

        // Group by mock section
        const sectionStats = {};
        MOCK_SECTIONS_SPEC.forEach(s => {
            sectionStats[s.name] = { total: s.count, correct: 0, wrong: 0, skipped: 0, icon: s.icon };
        });

        activeTest.reviewItems.forEach(r => {
            const sec = r.section || 'General';
            if (!sectionStats[sec]) sectionStats[sec] = { total: 0, correct: 0, wrong: 0, skipped: 0, icon: '📌' };
            if (r.isCorrect) sectionStats[sec].correct++;
            else if (r.isSkipped) sectionStats[sec].skipped++;
            else sectionStats[sec].wrong++;
        });

        const rowsHtml = Object.entries(sectionStats).map(([secName, s]) => {
            const marks = (s.correct * 2.0) - (s.wrong * 0.50);
            const maxMarks = s.total * 2;
            return `
                <tr>
                    <td><b>${s.icon} ${secName}</b></td>
                    <td>${s.total}</td>
                    <td style="color:#10b981; font-weight:700;">${s.correct}</td>
                    <td style="color:#ef4444; font-weight:700;">${s.wrong}</td>
                    <td style="font-weight:800; color:#818cf8;">${Math.max(0, marks).toFixed(1)} / ${maxMarks}</td>
                </tr>
            `;
        }).join('');

        tableContainer.innerHTML = `
            <div style="font-size:14px; font-weight:800; color:#f8fafc; margin-top:20px; text-align:left;">📊 Section-Wise Scorecard (SSC Marking Scheme):</div>
            <table class="mock-scorecard-table">
                <thead>
                    <tr>
                        <th>Subject Section</th>
                        <th>Questions</th>
                        <th>Correct (+2)</th>
                        <th>Wrong (-0.5)</th>
                        <th>Net Marks</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
            </table>
        `;
    }

    function renderReviewList() {
        const container = document.getElementById('resultsReviewList');
        if (!container) return;

        container.innerHTML = activeTest.reviewItems.map(item => {
            let statusClass = 'is-skipped';
            let statusBadge = '<span style="color:#f59e0b;">SKIPPED</span>';
            if (item.isCorrect) {
                statusClass = 'is-correct';
                statusBadge = '<span style="color:#10b981;">CORRECT ✓</span>';
            } else if (!item.isSkipped) {
                statusClass = 'is-wrong';
                statusBadge = '<span style="color:#ef4444;">INCORRECT ✕</span>';
            }

            const chosenText = item.selectedIndex >= 0 ? item.options[item.selectedIndex] : 'No Option Selected';
            const correctText = item.options[item.correctIndex];

            return `
                <div class="review-item-card ${statusClass}">
                    <div class="review-item-header">
                        <span>Q${item.questionNumber} • ${item.section || item.category}</span>
                        ${statusBadge}
                    </div>
                    <div class="review-item-qtext">${item.questionText}</div>
                    ${!item.isCorrect && item.selectedIndex >= 0 ? `
                        <div class="review-item-choice choice-your-wrong">
                            <b>Your Answer:</b> ${chosenText}
                        </div>
                    ` : ''}
                    <div class="review-item-choice choice-correct">
                        <b>Correct Answer:</b> ${correctText}
                    </div>
                </div>
            `;
        }).join('');
    }

    window.toggleResultsReview = function () {
        const reviewBox = document.getElementById('resultsReviewContainer');
        const btn = document.getElementById('btnToggleReview');
        if (!reviewBox || !btn) return;

        if (reviewBox.style.display === 'none') {
            reviewBox.style.display = 'block';
            btn.textContent = 'Hide Solutions ▲';
        } else {
            reviewBox.style.display = 'none';
            btn.textContent = 'Review All Questions & Answers ▼';
        }
    };

    window.retestCurrentSet = function () {
        if (!activeTest.questions || activeTest.questions.length === 0) return;
        launchWebTest(activeTest.questions, activeTest.title, activeTest.subtitle, activeTest.setKey, activeTest.is100QMock, activeTest.isExamMode, activeTest.mockLevel, activeTest.mockNum);
    };

    // 100-Question Palette Modal Controls
    window.openPaletteModal = function () {
        let modal = document.getElementById('paletteModal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'paletteModal';
            modal.className = 'palette-modal-overlay';
            modal.innerHTML = `
                <div class="palette-dialog-box">
                    <div class="palette-dialog-header">
                        <span class="palette-dialog-title">📊 100-Question Question Palette</span>
                        <button style="background:none; border:none; color:#94a3b8; font-size:18px; cursor:pointer;" onclick="window.closePaletteModal()">✕</button>
                    </div>
                    <div class="palette-legend-row">
                        <span class="legend-pill"><span class="legend-dot answered"></span> Answered</span>
                        <span class="legend-pill"><span class="legend-dot skipped"></span> Skipped</span>
                        <span class="legend-pill"><span class="legend-dot unvisited"></span> Unvisited</span>
                    </div>
                    <div class="palette-grid-scroll" id="paletteGridContainer">
                        <!-- Populated dynamically -->
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
        }

        const grid = document.getElementById('paletteGridContainer');
        if (grid) {
            grid.innerHTML = activeTest.questions.map((q, idx) => {
                let cellClass = '';
                if (idx === activeTest.currentIndex) cellClass += ' is-active';
                if (activeTest.userAnswers[idx] !== undefined) cellClass += ' is-answered';
                else if (activeTest.reviewItems.some(r => r.questionNumber === idx + 1 && r.isSkipped)) cellClass += ' is-skipped';

                return `
                    <button class="palette-num-btn ${cellClass}" onclick="window.jumpToQuestionFromPalette(${idx})">
                        ${idx + 1}
                    </button>
                `;
            }).join('');
        }

        modal.classList.add('active');
    };

    window.closePaletteModal = function () {
        const modal = document.getElementById('paletteModal');
        if (modal) modal.classList.remove('active');
    };

    window.jumpToQuestionFromPalette = function (index) {
        window.closePaletteModal();
        displayActiveQuestion(index);
    };

    window.toggleTestLanguage = async function () {
        activeTest.isHindiMode = !activeTest.isHindiMode;
        const btn = document.getElementById('btnTestTranslate');
        if (btn) {
            btn.classList.toggle('active-hindi', activeTest.isHindiMode);
            btn.textContent = activeTest.isHindiMode ? 'English / अ' : 'अ / A Hindi';
        }
        const q = activeTest.questions[activeTest.currentIndex];
        renderQuestionTextAndOptions(q);
    };

    window.confirmExitTest = function () {
        const modal = document.getElementById('webExitConfirmModal');
        if (modal) modal.classList.add('active');
        else if (confirm('Are you sure you want to exit? Your progress in this test will be lost.')) {
            closeWebTest();
        }
    };

    function closeWebTest() {
        clearInterval(activeTest.timerInterval);
        activeTest.isOpen = false;
        const modal = document.getElementById('webTestModal');
        if (modal) modal.classList.remove('active');
        document.body.style.overflow = '';
        renderLastPracticedBanner();
        renderTopicsGrid();
    }

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

        // If on mock-tests.html standalone page
        if (typeof renderMockCards === 'function') {
            renderMockCards();
        }

        renderPortalModeNav();
        renderMockHub();
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
