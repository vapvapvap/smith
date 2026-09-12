import { fetchModels, complete, streamCompletion } from './api.js';
import { CONTEXT_CHAR_LIMIT } from './config.js';
import { MessageView } from './render.js';
import {
    state,
    newId,
    loadState,
    persistSettings,
    persistMessages,
    persistTheme,
    resetMessages,
} from './state.js';

const el = (id) => document.getElementById(id);

const dom = {
    model: el('model'),
    stream: el('stream-toggle'),
    context: el('context-toggle'),
    newChat: el('new-chat'),
    clearChat: el('clear-chat'),
    messages: el('messages'),
    messagesList: el('messages-list'),
    emptyState: el('empty-state'),
    composer: el('composer'),
    prompt: el('prompt'),
    systemPrompt: el('system-prompt'),
    send: el('send'),
    themeToggle: el('theme-toggle'),
    sidebar: el('sidebar'),
    sidebarOpen: el('sidebar-open'),
    sidebarClose: el('sidebar-close'),
    backdrop: el('backdrop'),
    status: el('status'),
    statusText: el('status-text'),
    toast: el('toast'),
    chatTitle: el('chat-title'),
};

const numberFields = [
    'temperature',
    'top_p',
    'max_tokens',
    'top_k',
];

const views = new Map();
let toastTimer = null;

function applyTheme() {
    document.documentElement.dataset.theme = state.theme;
}

function setStatus(mode, text) {
    dom.status.classList.toggle('status--online', mode === 'online');
    dom.status.classList.toggle('status--offline', mode === 'offline');
    dom.statusText.textContent = text;
}

function showToast(message, isError = false) {
    dom.toast.textContent = message;
    dom.toast.classList.toggle('toast--error', isError);
    dom.toast.classList.add('toast--visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        dom.toast.classList.remove('toast--visible');
    }, 4000);
}

function isNearBottom() {
    const { scrollTop, scrollHeight, clientHeight } = dom.messages;
    return scrollHeight - scrollTop - clientHeight < 120;
}

function scrollToBottom() {
    dom.messages.scrollTop = dom.messages.scrollHeight;
}

function syncEmptyState() {
    dom.emptyState.hidden = state.messages.length > 0;
}

function appendMessageView(message) {
    const view = new MessageView(message);
    views.set(message.id, view);
    dom.messagesList.append(view.el);
    syncEmptyState();
    scrollToBottom();
    return view;
}

function updateMessageView(message, keepScroll = false) {
    const view = views.get(message.id);
    if (!view) {
        return;
    }
    const nearBottom = isNearBottom();
    view.update(message);
    if (!keepScroll || nearBottom) {
        scrollToBottom();
    }
}

function renderMessages() {
    views.clear();
    dom.messagesList.replaceChildren();
    for (const message of state.messages) {
        const view = new MessageView(message);
        views.set(message.id, view);
        dom.messagesList.append(view.el);
    }
    syncEmptyState();
    scrollToBottom();
}

function applySettingsToInputs() {
    const s = state.settings;
    dom.stream.checked = !!s.stream;
    dom.context.checked = !!s.context;
    el('thinking').value = s.thinking;
    el('reasoning_effort').value = s.reasoning_effort;
    el('stop').value = s.stop;
    dom.systemPrompt.value = s.systemPrompt ?? '';
    for (const key of numberFields) {
        el(key).value = s[key] ?? '';
    }
}

function populateModels() {
    dom.model.replaceChildren();
    const models = [...state.models].sort((a, b) => a.alias.localeCompare(b.alias));
    for (const model of models) {
        const option = document.createElement('option');
        option.value = model.alias;
        option.textContent = `${model.alias} · ${model.providerName}`;
        dom.model.append(option);
    }
    const known = state.models.some((m) => m.alias === state.settings.model);
    if (!known && models.length > 0) {
        state.settings.model = models[0].alias;
        persistSettings();
    }
    dom.model.value = state.settings.model;
    dom.model.disabled = state.models.length === 0;
}

async function loadModels() {
    try {
        state.models = await fetchModels();
        populateModels();
        setStatus('online', `Подключено · моделей: ${state.models.length}`);
    } catch (error) {
        setStatus('offline', 'Бэкенд недоступен');
        showToast(`Не удалось загрузить модели: ${error.message}`, true);
    }
}

function parseStop(raw) {
    if (!raw) {
        return [];
    }
    return raw
        .split(',')
        .map((item) => item.trim().replace(/\\n/g, '\n').replace(/\\t/g, '\t'))
        .filter(Boolean)
        .slice(0, 16);
}

function buildPrompt(history, current) {
    if (!state.settings.context) {
        return current;
    }
    const lines = [];
    for (const message of history) {
        if (message.role === 'user' && message.content) {
            lines.push(`Пользователь: ${message.content}`);
        } else if (message.role === 'assistant' && message.content && !message.error) {
            lines.push(`Ассистент: ${message.content}`);
        }
    }
    lines.push(`Пользователь: ${current}`);

    let text = lines.join('\n\n');
    while (text.length > CONTEXT_CHAR_LIMIT && lines.length > 1) {
        lines.shift();
        text = lines.join('\n\n');
    }
    return text;
}

function buildPayload(prompt) {
    const s = state.settings;
    const payload = {
        prompt,
        model: s.model,
        stream: !!s.stream,
        thinking: s.thinking,
        reasoning_effort: s.reasoning_effort,
    };
    for (const key of numberFields) {
        const raw = s[key];
        if (raw !== '' && raw != null) {
            const value = Number(raw);
            if (!Number.isNaN(value)) {
                payload[key] = value;
            }
        }
    }
    const stop = parseStop(s.stop);
    if (stop.length > 0) {
        payload.stop = stop;
    }
    const systemPrompt = (s.systemPrompt || '').trim();
    if (systemPrompt) {
        payload.system_prompt = systemPrompt;
    }
    return payload;
}

function setStreaming(active) {
    state.streaming = active;
    dom.send.disabled = active;
    dom.composer.classList.toggle('composer--busy', active);
}

async function send() {
    const text = dom.prompt.value.trim();
    if (!text || state.streaming) {
        return;
    }
    if (!state.settings.model) {
        showToast('Выберите модель', true);
        return;
    }

    const prompt = buildPrompt(state.messages, text);

    const userMessage = { id: newId(), role: 'user', content: text };
    state.messages.push(userMessage);
    appendMessageView(userMessage);

    const assistantMessage = {
        id: newId(),
        role: 'assistant',
        content: '',
        reasoningContent: '',
        pending: true,
    };
    state.messages.push(assistantMessage);
    appendMessageView(assistantMessage);
    persistMessages();

    dom.prompt.value = '';
    autoResize();
    setStreaming(true);

    const payload = buildPayload(prompt);
    try {
        if (state.settings.stream) {
            await streamCompletion(payload, {
                onChunk: (data) => {
                    assistantMessage.content += data?.content || '';
                    assistantMessage.reasoningContent += data?.reasoningContent || '';
                    updateMessageView(assistantMessage, true);
                },
                onUsage: (data) => {
                    assistantMessage.usage = data;
                    updateMessageView(assistantMessage, true);
                },
                onDone: (data) => {
                    assistantMessage.finishReason = data?.finishReason;
                    assistantMessage.pending = false;
                    updateMessageView(assistantMessage, true);
                },
                onError: (data) => {
                    assistantMessage.pending = false;
                    assistantMessage.error = true;
                    assistantMessage.content = data?.message || 'Ошибка провайдера';
                    updateMessageView(assistantMessage);
                },
            });
        } else {
            const result = await complete(payload);
            assistantMessage.content = result.content || '';
            assistantMessage.reasoningContent = result.reasoningContent || '';
            assistantMessage.usage = result.usage;
            assistantMessage.finishReason = result.finishReason;
        }
    } catch (error) {
        assistantMessage.error = true;
        assistantMessage.content = error.message || String(error);
        showToast(`Ошибка: ${error.message}`, true);
    } finally {
        assistantMessage.pending = false;
        updateMessageView(assistantMessage);
        persistMessages();
        setStreaming(false);
    }
}

function autoResizeField(field, maxHeight) {
    field.style.height = 'auto';
    field.style.height = `${Math.min(field.scrollHeight, maxHeight)}px`;
}

function autoResize() {
    autoResizeField(dom.prompt, 200);
}

function openSidebar() {
    dom.sidebar.classList.add('sidebar--open');
    dom.backdrop.classList.add('backdrop--visible');
}

function closeSidebar() {
    dom.sidebar.classList.remove('sidebar--open');
    dom.backdrop.classList.remove('backdrop--visible');
}

function bindSettings() {
    dom.stream.addEventListener('change', () => {
        state.settings.stream = dom.stream.checked;
        persistSettings();
    });
    dom.context.addEventListener('change', () => {
        state.settings.context = dom.context.checked;
        persistSettings();
    });
    dom.model.addEventListener('change', () => {
        state.settings.model = dom.model.value;
        persistSettings();
    });

    const selectFields = ['thinking', 'reasoning_effort', 'stop'];
    for (const key of [...numberFields, ...selectFields]) {
        el(key).addEventListener('input', (event) => {
            state.settings[key] = event.target.value;
            persistSettings();
        });
    }

    dom.systemPrompt.addEventListener('input', () => {
        state.settings.systemPrompt = dom.systemPrompt.value;
        persistSettings();
    });
}

function bindEvents() {
    dom.composer.addEventListener('submit', (event) => {
        event.preventDefault();
        send();
    });

    dom.prompt.addEventListener('input', autoResize);
    dom.prompt.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            send();
        }
    });

    const clear = () => {
        resetMessages();
        renderMessages();
        dom.chatTitle.textContent = 'Новый диалог';
        closeSidebar();
        dom.prompt.focus();
    };
    dom.newChat.addEventListener('click', clear);
    dom.clearChat.addEventListener('click', clear);

    dom.themeToggle.addEventListener('click', () => {
        state.theme = state.theme === 'dark' ? 'light' : 'dark';
        applyTheme();
        persistTheme();
    });

    dom.sidebarOpen.addEventListener('click', openSidebar);
    dom.sidebarClose.addEventListener('click', closeSidebar);
    dom.backdrop.addEventListener('click', closeSidebar);
}

function init() {
    loadState();
    applyTheme();
    applySettingsToInputs();
    bindSettings();
    bindEvents();
    renderMessages();
    autoResize();
    loadModels();
    dom.prompt.focus();
}

init();
