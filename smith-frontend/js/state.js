const SETTINGS_KEY = 'smith.settings';
const MESSAGES_KEY = 'smith.messages';
const THEME_KEY = 'smith.theme';

export const defaultSettings = {
    model: 'DEEPSEEK_V4_FLASH',
    stream: true,
    context: false,
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
    streaming: false,
    theme: 'light',
};

export function newId() {
    if (window.crypto && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
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

export function resetMessages() {
    state.messages = [];
    persistMessages();
}
