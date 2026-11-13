// Study Tables Reader: Subject Tabs -> Topic Cards -> Table Reader (matches Topic Sets UX)
(function () {
    'use strict';

    const SUBJECTS = [
        { key: 'static_gk', name: 'Static GK & Culture', desc: 'Dances, festivals, temples, parks, exercises', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>' },
        { key: 'science', name: 'Biology & Science', desc: 'Vitamins, diseases, organs, physics & chemistry', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3h6"/><path d="M10 3v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3"/></svg>' },
        { key: 'polity', name: 'Polity & Articles', desc: 'Constitution parts, schedules, amendments, bodies', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18"/><path d="M5 7h14"/><path d="M5 7l-2 6h4l-2-6z"/><path d="M19 7l-2 6h4l-2-6z"/></svg>' },
        { key: 'geography', name: 'Geography & Dams', desc: 'Rivers, dams, passes, soils, biosphere reserves', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21"/><line x1="9" y1="3" x2="9" y2="18"/><line x1="15" y1="6" x2="15" y2="21"/></svg>' },
        { key: 'history', name: 'History & Sites', desc: 'Indus Valley, dynasties, freedom movement, battles', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M3 22h18"/><path d="M6 18v-7"/><path d="M10 18v-7"/><path d="M14 18v-7"/><path d="M18 18v-7"/><polygon points="12 2 20 7 4 7"/></svg>' },
        { key: 'economics', name: 'Economics & RBI', desc: 'Five-year plans, RBI, budgets, schemes', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>' },
        { key: 'math', name: 'Mathematics', desc: 'Geometry, trigonometry, algebra, mensuration formulas', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2" width="16" height="20" rx="2"/><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="10" x2="10" y2="10"/><line x1="14" y1="10" x2="16" y2="10"/><line x1="8" y1="14" x2="10" y2="14"/><line x1="14" y1="14" x2="16" y2="14"/><line x1="8" y1="18" x2="16" y2="18"/></svg>' },
        { key: 'english', name: 'English Grammar', desc: 'Fixed prepositions, phrasal verbs, rules', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/></svg>' },
        { key: 'byjus_archive', name: "BYJU'S GK Tables", desc: 'Reference fact tables from static GK archive', icon: '<svg class="tab-svg-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>' }
    ];

    let allTables = [];
    let viewMode = 'topics'; // 'topics' | 'reader'
    let selectedSubject = 'static_gk';
    let selectedTableId = null;
    let searchQuery = '';
    let recallMode = false;

    function cleanCell(v) {
        let s = String(v === null || v === undefined ? '' : v).trim();
        if (/'[a-zA-Z0-9]'/.test(s) || s.includes("''")) {
            s = s.replace(/'/g, '');
        }
        return s.replace(/\ufffd/g, "'");
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function highlight(text, q) {
        const safe = escapeHtml(text);
        if (!q) return safe;
        const esc = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        try {
            return safe.replace(new RegExp(`(${esc})`, 'gi'), '<mark class="hl">$1</mark>');
        } catch (e) {
            return safe;
        }
    }

    function loadTables() {
        const curated = Array.isArray(window.QUIZEE_STUDY_TABLES) ? window.QUIZEE_STUDY_TABLES : [];
        allTables = curated.slice();

        if (window.BYJUS_GK_DATA && Array.isArray(window.BYJUS_GK_DATA.topics)) {
            window.BYJUS_GK_DATA.topics.forEach((topic) => {
                const stList = topic.studyTables || [];
                stList.forEach((tbl, idx) => {
                    const rows = (tbl.rows || []).map((r) => r.map(cleanCell));
                    const headers = (tbl.headers || []).map(cleanCell);
                    if (rows.length === 0) return;

                    const cols = Math.max(headers.length, rows[0] ? rows[0].length : 0);
                    const normH = [];
                    for (let c = 0; c < cols; c++) normH.push(headers[c] || `Col ${c + 1}`);
                    const normR = rows.map((r) => {
                        const out = [];
                        for (let c = 0; c < cols; c++) out.push(r[c] !== undefined ? r[c] : '');
                        return out;
                    });

                    allTables.push({
                        id: `byjus_${topic.id}_${idx + 1}`,
                        subject: 'byjus_archive',
                        subjectLabel: topic.category || "BYJU'S GK",
                        title: stList.length > 1 ? `${topic.title} (Part ${idx + 1})` : topic.title,
                        description: topic.description || '',
                        headers: normH,
                        rows: normR
                    });
                });
            });
        }
    }

    function getTablesForSubject(subKey) {
        return allTables.filter((t) => t.subject === subKey);
    }

    function renderTableHtml(table, rows, query) {
        const recallCls = recallMode ? 'recall-on' : '';
        return `
            <div class="reader-card ${recallCls}">
                <div class="reader-header">
                    <div>
                        <div class="reader-kicker">${escapeHtml(table.subjectLabel || '')} · ${rows.length} rows</div>
                        <h2 class="reader-title">${highlight(table.title, query)}</h2>
                        ${table.description ? `<p class="reader-desc">${highlight(table.description, query)}</p>` : ''}
                    </div>
                </div>
                ${recallMode ? '<div class="recall-banner">Recall mode on — tap or hover blurred cells to reveal answers</div>' : ''}
                <div class="table-scroll-x">
                    <table class="clean-table">
                        <thead>
                            <tr>
                                ${table.headers.map((h) => `<th>${highlight(h, query)}</th>`).join('')}
                            </tr>
                        </thead>
                        <tbody>
                            ${rows.map((r) => `
                                <tr>
                                    ${r.map((cell) => `<td>${highlight(cell, query)}</td>`).join('')}
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    function wireRecallCells(container) {
        container.querySelectorAll('table.clean-table tbody td:not(:first-child)').forEach((td) => {
            td.addEventListener('click', () => {
                if (recallMode) td.classList.toggle('revealed');
            });
        });
    }

    function renderBreadcrumb() {
        const bc = document.getElementById('tablesBreadcrumb');
        const recallBtn = document.getElementById('btnRecallToggle');
        if (!bc) return;

        if (recallBtn) {
            const showRecall = viewMode === 'reader' || searchQuery.trim().length > 0;
            recallBtn.style.display = showRecall ? 'inline-flex' : 'none';
        }

        if (searchQuery.trim().length > 0) {
            bc.style.display = 'none';
            bc.innerHTML = '';
            return;
        }

        const subMeta = SUBJECTS.find((s) => s.key === selectedSubject) || SUBJECTS[0];

        if (viewMode === 'topics') {
            bc.style.display = 'flex';
            bc.className = 'subject-tabs-row';
            bc.style.marginBottom = '22px';
            bc.innerHTML = SUBJECTS.map((s) => {
                const cnt = getTablesForSubject(s.key).length;
                const active = s.key === selectedSubject ? 'active' : '';
                return `
                    <button type="button" class="subject-tab-btn ${active}" data-sub="${s.key}">
                        ${s.icon}
                        <span>${escapeHtml(s.name)}</span>
                        <span class="tab-count-badge">${cnt}</span>
                    </button>
                `;
            }).join('');

            bc.querySelectorAll('.subject-tab-btn').forEach((btn) => {
                btn.addEventListener('click', () => goTopics(btn.getAttribute('data-sub')));
            });
            return;
        }

        if (viewMode === 'reader') {
            const activeTable = allTables.find((t) => t.id === selectedTableId);
            bc.style.display = 'flex';
            bc.className = 'drill-breadcrumb';
            bc.style.marginBottom = '20px';
            bc.innerHTML = `
                <button type="button" class="drill-back-btn" id="bcBackTopics">← ${escapeHtml(subMeta.name)}</button>
                <span class="drill-sep">/</span>
                <span class="drill-current">${activeTable ? escapeHtml(activeTable.title) : 'Table'}</span>
            `;
            document.getElementById('bcBackTopics').addEventListener('click', () => goTopics(selectedSubject));
        }
    }

    function renderStage() {
        const stage = document.getElementById('tablesStage');
        if (!stage) return;

        renderBreadcrumb();
        const q = searchQuery.trim();

        // Search Mode
        if (q.length > 0) {
            const ql = q.toLowerCase();
            const matches = [];
            allTables.forEach((t) => {
                const metaMatch = `${t.title} ${t.subjectLabel || ''} ${t.description || ''}`.toLowerCase().includes(ql);
                const rowMatches = t.rows.filter((r) => r.some((c) => String(c).toLowerCase().includes(ql)));
                if (metaMatch || rowMatches.length > 0) {
                    matches.push({ table: t, rows: rowMatches.length > 0 ? rowMatches : t.rows });
                }
            });

            if (matches.length === 0) {
                stage.innerHTML = `<div class="empty-box"><p>No tables or rows matched "<strong>${escapeHtml(q)}</strong>".</p></div>`;
                return;
            }

            const shown = matches.slice(0, 20);
            const totalRowHits = matches.reduce((s, m) => s + m.rows.length, 0);
            stage.innerHTML = `
                <div class="search-banner">
                    Found <strong>${matches.length}</strong> matching tables (${totalRowHits} rows)${matches.length > 20 ? ' — showing top 20' : ''}
                </div>
                ${shown.map((m) => renderTableHtml(m.table, m.rows, q)).join('')}
            `;
            wireRecallCells(stage);
            return;
        }

        // Step 1: Topics Grid for Selected Subject
        if (viewMode === 'topics' || viewMode === 'subjects') {
            const subMeta = SUBJECTS.find((s) => s.key === selectedSubject) || SUBJECTS[0];
            const topics = getTablesForSubject(subMeta.key);
            if (topics.length === 0) {
                stage.innerHTML = `<div class="empty-box"><p>No tables available in this subject.</p></div>`;
                return;
            }

            stage.innerHTML = `
                <div class="drill-grid">
                    ${topics.map((t) => `
                        <button type="button" class="drill-card" data-id="${t.id}">
                            <div class="drill-card-top">
                                <div style="display:flex; align-items:center; gap:8px;">
                                    <span class="card-icon-badge">${subMeta.icon}</span>
                                    <span class="drill-card-meta">${t.rows.length} rows</span>
                                </div>
                                <span class="drill-card-arrow">→</span>
                            </div>
                            <div class="drill-card-title">${escapeHtml(t.title)}</div>
                        </button>
                    `).join('')}
                </div>
            `;
            stage.querySelectorAll('.drill-card[data-id]').forEach((btn) => {
                btn.addEventListener('click', () => goReader(btn.getAttribute('data-id')));
            });
            return;
        }

        // Step 3: Clean Full-Width Table Reader
        const topics = getTablesForSubject(selectedSubject);
        const currentIdx = Math.max(0, topics.findIndex((t) => t.id === selectedTableId));
        const activeTable = topics[currentIdx];

        if (!activeTable) {
            goTopics(selectedSubject);
            return;
        }

        const prevTopic = currentIdx > 0 ? topics[currentIdx - 1] : null;
        const nextTopic = currentIdx < topics.length - 1 ? topics[currentIdx + 1] : null;

        stage.innerHTML = `
            ${renderTableHtml(activeTable, activeTable.rows, '')}
            <div class="reader-nav-footer">
                <button type="button" class="btn-topic-nav" id="btnPrevTopic" ${prevTopic ? '' : 'disabled'}>
                    ← ${prevTopic ? escapeHtml(prevTopic.title) : 'Start'}
                </button>
                <button type="button" class="btn-topic-nav" id="btnNextTopic" ${nextTopic ? '' : 'disabled'}>
                    ${nextTopic ? escapeHtml(nextTopic.title) : 'End'} →
                </button>
            </div>
        `;
        wireRecallCells(stage);

        const prevBtn = document.getElementById('btnPrevTopic');
        const nextBtn = document.getElementById('btnNextTopic');
        if (prevBtn && prevTopic) prevBtn.addEventListener('click', () => goReader(prevTopic.id));
        if (nextBtn && nextTopic) nextBtn.addEventListener('click', () => goReader(nextTopic.id));
    }

    function goSubjects() {
        viewMode = 'subjects';
        selectedSubject = null;
        selectedTableId = null;
        renderStage();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function goTopics(subKey) {
        selectedSubject = subKey;
        selectedTableId = null;
        viewMode = 'topics';
        renderStage();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function goReader(tableId) {
        selectedTableId = tableId;
        viewMode = 'reader';
        renderStage();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function setupEvents() {
        const searchInp = document.getElementById('tablesSearchInput');
        const clearBtn = document.getElementById('btnClearSearch');
        const recallBtn = document.getElementById('btnRecallToggle');
        const backTop = document.getElementById('btnBackToTop');
        const mobileMenuBtn = document.getElementById('tablesMobileToggle');
        const mobileDrawer = document.getElementById('tablesMobileDrawer');

        if (searchInp) {
            searchInp.addEventListener('input', (e) => {
                searchQuery = e.target.value;
                if (clearBtn) clearBtn.classList.toggle('visible', searchQuery.trim().length > 0);
                renderStage();
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                searchQuery = '';
                if (searchInp) searchInp.value = '';
                clearBtn.classList.remove('visible');
                renderStage();
            });
        }

        if (recallBtn) {
            recallBtn.addEventListener('click', () => {
                recallMode = !recallMode;
                recallBtn.classList.toggle('active', recallMode);
                recallBtn.textContent = recallMode ? 'Show Answers' : 'Hide Answers';
                renderStage();
            });
        }

        if (backTop) {
            window.addEventListener('scroll', () => {
                backTop.classList.toggle('visible', window.scrollY > 320);
            });
            backTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
        }

        if (mobileMenuBtn && mobileDrawer) {
            mobileMenuBtn.addEventListener('click', () => mobileDrawer.classList.toggle('open'));
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        loadTables();
        setupEvents();
        renderStage();
    });
})();
