// Clean Master-Detail Study Tables Reader
(function () {
    'use strict';

    const SUBJECTS = [
        { key: 'static_gk', name: 'Static GK & Culture', icon: '🏛️' },
        { key: 'science', name: 'Biology & Science', icon: '🧬' },
        { key: 'polity', name: 'Polity & Articles', icon: '⚖️' },
        { key: 'geography', name: 'Geography & Dams', icon: '🌍' },
        { key: 'history', name: 'History & Sites', icon: '🏺' },
        { key: 'economics', name: 'Economics & RBI', icon: '📈' },
        { key: 'math', name: 'Mathematics', icon: '📐' },
        { key: 'english', name: 'English Grammar', icon: '📖' },
        { key: 'byjus_archive', name: "BYJU'S GK Tables", icon: '📚' }
    ];

    let allTables = [];
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
                        subjectLabel: `BYJU'S Archive • ${topic.category || 'GK'}`,
                        title: stList.length > 1 ? `${topic.title} (Part ${idx + 1})` : topic.title,
                        icon: topic.icon || '📌',
                        description: topic.description || '',
                        headers: normH,
                        rows: normR
                    });
                });
            });
        }

        const firstInSub = getTablesForSubject(selectedSubject)[0];
        selectedTableId = firstInSub ? firstInSub.id : (allTables[0] ? allTables[0].id : null);
    }

    function getTablesForSubject(subKey) {
        return allTables.filter((t) => t.subject === subKey);
    }

    function renderSidebarAndMobile() {
        const subWrap = document.getElementById('sidebarSubjects');
        const topicWrap = document.getElementById('sidebarTopics');
        const topicHeading = document.getElementById('sidebarTopicHeading');
        const mSub = document.getElementById('mobileSubjectSelect');
        const mTopic = document.getElementById('mobileTopicSelect');

        // 1. Desktop Subjects
        if (subWrap) {
            subWrap.innerHTML = SUBJECTS.map((s) => {
                const cnt = getTablesForSubject(s.key).length;
                const active = s.key === selectedSubject ? 'active' : '';
                return `
                    <button type="button" class="sb-subject-btn ${active}" data-sub="${s.key}">
                        <span class="sb-subject-left"><span>${s.icon}</span><span>${escapeHtml(s.name)}</span></span>
                        <span class="sb-count">${cnt}</span>
                    </button>
                `;
            }).join('');

            subWrap.querySelectorAll('.sb-subject-btn').forEach((btn) => {
                btn.addEventListener('click', () => {
                    selectSubject(btn.getAttribute('data-sub'));
                });
            });
        }

        // 2. Desktop Topics for active subject
        const topics = getTablesForSubject(selectedSubject);
        const subMeta = SUBJECTS.find((s) => s.key === selectedSubject);
        if (topicHeading && subMeta) {
            topicHeading.textContent = `${subMeta.name} (${topics.length})`;
        }

        if (topicWrap) {
            topicWrap.innerHTML = topics.map((t) => {
                const active = t.id === selectedTableId ? 'active' : '';
                return `
                    <button type="button" class="sb-topic-btn ${active}" data-id="${t.id}">
                        ${escapeHtml(t.title)}
                    </button>
                `;
            }).join('');

            topicWrap.querySelectorAll('.sb-topic-btn').forEach((btn) => {
                btn.addEventListener('click', () => {
                    selectTable(btn.getAttribute('data-id'));
                });
            });
        }

        // 3. Mobile Selectors
        if (mSub) {
            mSub.innerHTML = SUBJECTS.map((s) =>
                `<option value="${s.key}" ${s.key === selectedSubject ? 'selected' : ''}>${s.icon} ${escapeHtml(s.name)}</option>`
            ).join('');
        }
        if (mTopic) {
            mTopic.innerHTML = topics.map((t) =>
                `<option value="${t.id}" ${t.id === selectedTableId ? 'selected' : ''}>${escapeHtml(t.title)}</option>`
            ).join('');
        }
    }

    function renderTableHtml(table, rows, query) {
        const recallCls = recallMode ? 'recall-on' : '';
        return `
            <div class="reader-card ${recallCls}">
                <div class="reader-header">
                    <div>
                        <div class="reader-kicker">${escapeHtml(table.subjectLabel)} • ${rows.length} rows</div>
                        <h2 class="reader-title">${table.icon || '📋'} ${highlight(table.title, query)}</h2>
                        ${table.description ? `<p class="reader-desc">${highlight(table.description, query)}</p>` : ''}
                    </div>
                </div>
                ${recallMode ? '<div class="recall-banner">🧠 Memorize Mode ON — Tap or hover blurred cells to reveal answers</div>' : ''}
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

    function renderReader() {
        const reader = document.getElementById('tablesReader');
        if (!reader) return;

        const q = searchQuery.trim();

        // Mode A: Search Active
        if (q.length > 0) {
            const ql = q.toLowerCase();
            const matches = [];
            allTables.forEach((t) => {
                const metaMatch = `${t.title} ${t.subjectLabel} ${t.description || ''}`.toLowerCase().includes(ql);
                const rowMatches = t.rows.filter((r) => r.some((c) => String(c).toLowerCase().includes(ql)));
                if (metaMatch || rowMatches.length > 0) {
                    matches.push({ table: t, rows: rowMatches.length > 0 ? rowMatches : t.rows });
                }
            });

            if (matches.length === 0) {
                reader.innerHTML = `
                    <div class="empty-box">
                        <p>No tables or rows matched "<strong>${escapeHtml(q)}</strong>". Try another keyword.</p>
                    </div>
                `;
                return;
            }

            const shown = matches.slice(0, 20);
            const totalRowHits = matches.reduce((s, m) => s + m.rows.length, 0);
            reader.innerHTML = `
                <div class="search-banner">
                    Found <strong>${matches.length}</strong> matching tables (${totalRowHits} rows) for "<strong>${escapeHtml(q)}</strong>"${matches.length > 20 ? ' — showing top 20' : ''}
                </div>
                ${shown.map((m) => renderTableHtml(m.table, m.rows, q)).join('')}
            `;
            wireRecallCells(reader);
            return;
        }

        // Mode B: Clean Single Topic View
        const topics = getTablesForSubject(selectedSubject);
        const currentIdx = Math.max(0, topics.findIndex((t) => t.id === selectedTableId));
        const activeTable = topics[currentIdx];

        if (!activeTable) {
            reader.innerHTML = `<div class="empty-box"><p>No tables available for this subject.</p></div>`;
            return;
        }

        const prevTopic = currentIdx > 0 ? topics[currentIdx - 1] : null;
        const nextTopic = currentIdx < topics.length - 1 ? topics[currentIdx + 1] : null;

        reader.innerHTML = `
            ${renderTableHtml(activeTable, activeTable.rows, '')}
            <div class="reader-nav-footer">
                <button type="button" class="btn-topic-nav" id="btnPrevTopic" ${prevTopic ? '' : 'disabled'}>
                    ← ${prevTopic ? escapeHtml(prevTopic.title) : 'Start of Subject'}
                </button>
                <button type="button" class="btn-topic-nav" id="btnNextTopic" ${nextTopic ? '' : 'disabled'}>
                    ${nextTopic ? escapeHtml(nextTopic.title) : 'End of Subject'} →
                </button>
            </div>
        `;

        wireRecallCells(reader);

        const prevBtn = document.getElementById('btnPrevTopic');
        const nextBtn = document.getElementById('btnNextTopic');
        if (prevBtn && prevTopic) {
            prevBtn.addEventListener('click', () => selectTable(prevTopic.id, true));
        }
        if (nextBtn && nextTopic) {
            nextBtn.addEventListener('click', () => selectTable(nextTopic.id, true));
        }
    }

    function selectSubject(subKey) {
        selectedSubject = subKey;
        const first = getTablesForSubject(subKey)[0];
        selectedTableId = first ? first.id : null;
        // Clear search when choosing a subject so user enters clean reader mode
        if (searchQuery) {
            searchQuery = '';
            const inp = document.getElementById('tablesSearchInput');
            const clr = document.getElementById('btnClearSearch');
            if (inp) inp.value = '';
            if (clr) clr.classList.remove('visible');
        }
        renderSidebarAndMobile();
        renderReader();
    }

    function selectTable(tableId, scrollUp) {
        selectedTableId = tableId;
        if (searchQuery) {
            searchQuery = '';
            const inp = document.getElementById('tablesSearchInput');
            const clr = document.getElementById('btnClearSearch');
            if (inp) inp.value = '';
            if (clr) clr.classList.remove('visible');
        }
        renderSidebarAndMobile();
        renderReader();
        if (scrollUp) {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }

    function setupEvents() {
        const searchInp = document.getElementById('tablesSearchInput');
        const clearBtn = document.getElementById('btnClearSearch');
        const recallBtn = document.getElementById('btnRecallToggle');
        const mSub = document.getElementById('mobileSubjectSelect');
        const mTopic = document.getElementById('mobileTopicSelect');
        const backTop = document.getElementById('btnBackToTop');
        const mobileMenuBtn = document.getElementById('tablesMobileToggle');
        const mobileDrawer = document.getElementById('tablesMobileDrawer');

        if (searchInp) {
            searchInp.addEventListener('input', (e) => {
                searchQuery = e.target.value;
                if (clearBtn) clearBtn.classList.toggle('visible', searchQuery.trim().length > 0);
                renderReader();
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                searchQuery = '';
                if (searchInp) searchInp.value = '';
                clearBtn.classList.remove('visible');
                renderReader();
            });
        }

        if (recallBtn) {
            recallBtn.addEventListener('click', () => {
                recallMode = !recallMode;
                recallBtn.classList.toggle('active', recallMode);
                recallBtn.textContent = recallMode ? '👁️ Show Answers' : '🧠 Hide Answers';
                renderReader();
            });
        }

        if (mSub) {
            mSub.addEventListener('change', (e) => selectSubject(e.target.value));
        }

        if (mTopic) {
            mTopic.addEventListener('change', (e) => selectTable(e.target.value, true));
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
        renderSidebarAndMobile();
        renderReader();
    });
})();
