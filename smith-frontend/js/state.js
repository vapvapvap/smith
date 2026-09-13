const SETTINGS_KEY = 'smith.settings';
const MESSAGES_KEY = 'smith.messages';
const THEME_KEY = 'smith.theme';
const SUMMARY_KEY = 'smith.summary';
const FACTS_KEY = 'smith.facts';
const BRANCHES_KEY = 'smith.branches';

export const CONTEXT_STRATEGIES = {
    AS_IS: 'as_is',
    SLIDING_WINDOW: 'sliding_window',
    STICKY_FACTS: 'sticky_facts',
    BRANCHING: 'branching',
    SUMMARIZE: 'summarize',
};

export const defaultSettings = {
    model: 'DEEPSEEK_FLASH',
    context: false,
    contextStrategy: CONTEXT_STRATEGIES.AS_IS,
    slidingWindowSize: 10,
    summaryInterval: 10,
    temperature: '',
    top_p: '',
    max_tokens: '',
    top_k: '',
    thinking: 'enabled',
    reasoning_effort: 'high',
    stop: '',
    systemPrompt: '',
};

export const state = {
    models: [],
    messages: [],
    branches: [],
    activeBranchId: null,
    settings: { ...defaultSettings },
    summary: { text: '', coveredCount: 0, usage: null },
    facts: [],
    factsUsage: null,
    factsCoveredCount: 0,
    streaming: false,
    summarizing: false,
    updatingFacts: false,
    theme: 'light',
};

export function newId() {
    if (window.crypto && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function isDialogMessage(message) {
    return message.role === 'user' || message.role === 'assistant';
}

function normalizeBranch(branch, index) {
    return {
        id: branch && branch.id ? branch.id : newId(),
        name: branch && branch.name ? branch.name : `Ветка ${index + 1}`,
        checkpointId: branch && branch.checkpointId ? branch.checkpointId : null,
        messages: Array.isArray(branch && branch.messages)
            ? branch.messages.map((m) => ({ ...m, pending: false }))
            : [],
    };
}

function createBranch(name, messages = [], checkpointId = null) {
    return {
        id: newId(),
        name,
        checkpointId,
        messages,
    };
}

export function activeBranch() {
    return state.branches.find((branch) => branch.id === state.activeBranchId)
        || state.branches[0]
        || null;
}

function syncMessagesRef() {
    const branch = activeBranch();
    state.messages = branch ? branch.messages : [];
}

function loadBranches() {
    let loaded = false;
    try {
        const saved = JSON.parse(localStorage.getItem(BRANCHES_KEY) || 'null');
        if (saved && Array.isArray(saved.branches) && saved.branches.length > 0) {
            state.branches = saved.branches.map(normalizeBranch);
            const ids = state.branches.map((branch) => branch.id);
            state.activeBranchId = ids.includes(saved.activeBranchId)
                ? saved.activeBranchId
                : state.branches[0].id;
            loaded = true;
        }
    } catch {
        /* ignore corrupted storage */
    }

    if (!loaded) {
        let legacy = [];
        try {
            const savedMessages = JSON.parse(localStorage.getItem(MESSAGES_KEY) || 'null');
            if (Array.isArray(savedMessages)) {
                legacy = savedMessages.map((m) => ({ ...m, pending: false }));
            }
        } catch {
            /* ignore corrupted storage */
        }
        const branch = createBranch('Ветка 1', legacy);
        state.branches = [branch];
        state.activeBranchId = branch.id;
    }

    syncMessagesRef();
}

export function loadState() {
    try {
        const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) || 'null');
        if (saved && typeof saved === 'object') {
            state.settings = { ...defaultSettings, ...saved };
            if (saved.summarize === true && !saved.contextStrategy) {
                state.settings.contextStrategy = CONTEXT_STRATEGIES.SUMMARIZE;
            }
        }
    } catch {
        /* ignore corrupted storage */
    }

    if (!Object.values(CONTEXT_STRATEGIES).includes(state.settings.contextStrategy)) {
        state.settings.contextStrategy = CONTEXT_STRATEGIES.AS_IS;
    }

    loadBranches();

    try {
        const saved = JSON.parse(localStorage.getItem(SUMMARY_KEY) || 'null');
        if (saved && typeof saved === 'object') {
            const dialogCount = state.messages.filter(isDialogMessage).length;
            state.summary = {
                text: typeof saved.text === 'string' ? saved.text : '',
                coveredCount: Math.min(Number(saved.coveredCount) || 0, dialogCount),
                usage: saved.usage && typeof saved.usage === 'object' ? saved.usage : null,
            };
        }
    } catch {
        /* ignore corrupted storage */
    }

    try {
        const saved = JSON.parse(localStorage.getItem(FACTS_KEY) || 'null');
        if (saved && typeof saved === 'object') {
            state.facts = Array.isArray(saved.facts)
                ? saved.facts.filter((f) => f && typeof f === 'object')
                : [];
            state.factsUsage = saved.usage && typeof saved.usage === 'object' ? saved.usage : null;
            state.factsCoveredCount = Math.min(Number(saved.coveredCount) || 0, state.messages.filter(isDialogMessage).length);
        }
    } catch {
        /* ignore corrupted storage */
    }

    const storedTheme = localStorage.getItem(THEME_KEY);
    state.theme = storedTheme
        || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
}

export function persistSettings() {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settings));
}

export function persistMessages() {
    persistBranches();
}

export function persistBranches() {
    const branch = activeBranch();
    if (branch) {
        branch.messages = state.messages;
    }
    localStorage.setItem(BRANCHES_KEY, JSON.stringify({
        activeBranchId: state.activeBranchId,
        branches: state.branches,
    }));
}

export function persistTheme() {
    localStorage.setItem(THEME_KEY, state.theme);
}

export function persistSummary() {
    localStorage.setItem(SUMMARY_KEY, JSON.stringify(state.summary));
}

export function persistFacts() {
    localStorage.setItem(FACTS_KEY, JSON.stringify({
        facts: state.facts,
        usage: state.factsUsage,
        coveredCount: state.factsCoveredCount,
    }));
}

export function resetMessages() {
    const branch = createBranch('Ветка 1');
    state.branches = [branch];
    state.activeBranchId = branch.id;
    state.messages = branch.messages;
    persistBranches();
}

export function resetSummary() {
    state.summary = { text: '', coveredCount: 0, usage: null };
    localStorage.removeItem(SUMMARY_KEY);
}

export function resetFacts() {
    state.facts = [];
    state.factsUsage = null;
    state.factsCoveredCount = 0;
    localStorage.removeItem(FACTS_KEY);
}

export function switchBranch(branchId) {
    if (!state.branches.some((branch) => branch.id === branchId)) {
        return false;
    }
    persistBranches();
    state.activeBranchId = branchId;
    syncMessagesRef();
    persistBranches();
    return true;
}

export function forkBranchAt(messageId) {
    const source = activeBranch();
    if (!source) {
        return null;
    }
    const index = source.messages.findIndex((message) => message.id === messageId);
    if (index === -1) {
        return null;
    }
    const prefix = source.messages.slice(0, index + 1).map((message) => ({ ...message }));
    const branch = createBranch(`Ветка ${state.branches.length + 1}`, prefix, messageId);
    state.branches.push(branch);
    persistBranches();
    return branch;
}

export function factsToText(facts) {
    return (facts || [])
        .map((fact) => `${fact.key}: ${fact.value}`)
        .join('\n');
}

export function parseFactsText(text) {
    if (!text) {
        return [];
    }
    const result = [];
    for (const rawLine of String(text).split(/\r?\n/)) {
        const line = rawLine.trim().replace(/^[-*•]\s*/, '').replace(/^#+\s*/, '');
        if (!line) {
            continue;
        }
        const colon = line.search(/[:：]/);
        if (colon === -1) {
            continue;
        }
        const key = line.slice(0, colon).trim();
        const value = line.slice(colon + 1).trim();
        if (key && value) {
            result.push({ key, value });
        }
    }
    return result;
}
