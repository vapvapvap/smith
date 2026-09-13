const SETTINGS_KEY = 'smith.settings';
const MESSAGES_KEY = 'smith.messages';
const THEME_KEY = 'smith.theme';
const SUMMARY_KEY = 'smith.summary';

export const defaultSettings = {
    model: 'DEEPSEEK_FLASH',
    context: false,
    summarize: false,
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
    settings: { ...defaultSettings },
    summary: { text: '', coveredCount: 0, usage: null },
    streaming: false,
    summarizing: false,
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

export function loadState() {
    try {
        const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) || 'null');
        if (saved && typeof saved === 'object') {
            state.settings = { ...defaultSettings, ...saved };
        }
    } catch {
        /* ignore corrupted storage */
    }

    try {
        const saved = JSON.parse(localStorage.getItem(MESSAGES_KEY) || 'null');
        if (Array.isArray(saved)) {
            state.messages = saved.map((m) => ({ ...m, pending: false }));
        }
    } catch {
        /* ignore corrupted storage */
    }

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

    const storedTheme = localStorage.getItem(THEME_KEY);
    state.theme = storedTheme
        || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
}

export function persistSettings() {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settings));
}

export function persistMessages() {
    localStorage.setItem(MESSAGES_KEY, JSON.stringify(state.messages));
}

export function persistTheme() {
    localStorage.setItem(THEME_KEY, state.theme);
}

export function persistSummary() {
    localStorage.setItem(SUMMARY_KEY, JSON.stringify(state.summary));
}

export function resetMessages() {
    state.messages = [];
    persistMessages();
}

export function resetSummary() {
    state.summary = { text: '', coveredCount: 0, usage: null };
    localStorage.removeItem(SUMMARY_KEY);
}
