import { fetchModels, streamCompletion, summarizeCompletion, me, logout } from './api.js';
import { initAttachments } from './attachments.js';
import { CONTEXT_CHAR_LIMIT } from './config.js';
import { MessageView } from './render.js';
import {
    state,
    defaultSettings,
    newId,
    loadState,
    persistSettings,
    persistMessages,
    persistSummary,
    persistTheme,
    resetMessages,
    resetSummary,
    isDialogMessage,
} from './state.js';

const el = (id) => document.getElementById(id);

const dom = {
    model: el('model'),
    context: el('context-toggle'),
    summarize: el('summarize-toggle'),
    summaryInterval: el('summary-interval'),
    newChat: el('new-chat'),
    clearChat: el('clear-chat'),
    messages: el('messages'),
    messagesList: el('messages-list'),
    emptyState: el('empty-state'),
    tokenStats: el('token-stats'),
    statPrompt: el('stat-prompt'),
    statHistory: el('stat-history'),
    statCompletion: el('stat-completion'),
    statSummary: el('stat-summary'),
    statSummaryItem: el('stat-summary-item'),
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
    user: el('current-user'),
    logout: el('logout'),
};

const numberFields = [
    'temperature',
    'top_p',
    'max_tokens',
    'top_k',
];

const views = new Map();
let toastTimer = null;
let attachments = null;

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

function updateTokenStats() {
    const usages = state.messages
        .filter((m) => m.role === 'assistant' && m.usage)
        .map((m) => m.usage);

    const summaryUsage = state.summary.usage;
    const summaryTotal = summaryUsage
        ? (summaryUsage.promptTokens ?? 0) + (summaryUsage.completionTokens ?? 0)
        : 0;

    if (usages.length === 0 && summaryTotal === 0) {
        dom.tokenStats.hidden = true;
        return;
    }

    const last = usages[usages.length - 1];
    const history = usages.reduce((sum, usage) => {
        const total = (usage.promptTokens ?? 0) + (usage.completionTokens ?? 0);
        return sum + total;
    }, summaryTotal);

    dom.statPrompt.textContent = last ? (last.promptTokens ?? '—') : '—';
    dom.statHistory.textContent = history;
    dom.statCompletion.textContent = last ? (last.completionTokens ?? '—') : '—';

    if (summaryUsage) {
        dom.statSummaryItem.hidden = false;
        dom.statSummary.textContent = summaryTotal;
    } else {
        dom.statSummaryItem.hidden = true;
    }

    dom.tokenStats.hidden = false;
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
    updateTokenStats();
}

function updateSummaryControls() {
    const contextOn = dom.context.checked;
    dom.summarize.disabled = !contextOn;
    dom.summaryInterval.disabled = !(contextOn && dom.summarize.checked);
}

function applySettingsToInputs() {
    const s = state.settings;
    dom.context.checked = !!s.context;
    dom.summarize.checked = !!s.summarize;
    dom.summaryInterval.value = s.summaryInterval ?? defaultSettings.summaryInterval;
    el('thinking').value = s.thinking;
    el('reasoning_effort').value = s.reasoning_effort;
    el('stop').value = s.stop;
    dom.systemPrompt.value = s.systemPrompt ?? '';
    for (const key of numberFields) {
        el(key).value = s[key] ?? '';
    }
    updateSummaryControls();
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
        const preferred = state.models.some((m) => m.alias === defaultSettings.model)
            ? defaultSettings.model
            : models[0].alias;
        state.settings.model = preferred;
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

function dialogContentLines(messages) {
    const lines = [];
    for (const message of messages) {
        if (message.role === 'user' && message.content) {
            lines.push(`Пользователь: ${message.content}`);
        } else if (message.role === 'assistant' && message.content && !message.error) {
            lines.push(`Ассистент: ${message.content}`);
        }
    }
    return lines;
}

function summaryInterval() {
    const value = Math.floor(Number(state.settings.summaryInterval));
    return Number.isFinite(value) && value >= 1 ? value : defaultSettings.summaryInterval;
}

function buildPrompt(history, current) {
    if (!state.settings.context) {
        return current;
    }
    const lines = [];
    let source = history;
    if (state.settings.summarize && state.summary.text) {
        lines.push(`Саммари предыдущего диалога:\n${state.summary.text}`);
        source = history.filter(isDialogMessage).slice(state.summary.coveredCount);
    }
    lines.push(...dialogContentLines(source));
    lines.push(`Пользователь: ${current}`);

    let text = lines.join('\n\n');
    while (text.length > CONTEXT_CHAR_LIMIT && lines.length > 1) {
        lines.shift();
        text = lines.join('\n\n');
    }
    return text;
}

function accumulateUsage(total, usage) {
    if (!usage) {
        return total;
    }
    return {
        promptTokens: (total?.promptTokens || 0) + (usage.promptTokens || 0),
        completionTokens: (total?.completionTokens || 0) + (usage.completionTokens || 0),
        totalTokens: (total?.totalTokens || 0) + (usage.totalTokens || 0),
    };
}

async function maybeSummarize() {
    const s = state.settings;
    if (!s.context || !s.summarize || state.summarizing) {
        return;
    }
    const dialog = state.messages.filter(isDialogMessage);
    const uncovered = dialog.slice(state.summary.coveredCount);
    if (uncovered.length < summaryInterval()) {
        return;
    }

    const lines = [];
    if (state.summary.text) {
        lines.push(`Предыдущее саммари:\n${state.summary.text}`);
    }
    lines.push(...dialogContentLines(uncovered));
    const text = lines.join('\n\n');

    const systemMessage = {
        id: newId(),
        role: 'system',
        content: '',
        pending: true,
    };
    state.messages.push(systemMessage);
    appendMessageView(systemMessage);
    persistMessages();

    state.summarizing = true;
    try {
        const result = await summarizeCompletion({ text, model: s.model });
        systemMessage.content = result?.summary || '';
        systemMessage.pending = false;
        systemMessage.usage = result?.usage || null;
        state.summary = {
            text: result?.summary || '',
            coveredCount: dialog.length,
            usage: accumulateUsage(state.summary.usage, result?.usage),
        };
        persistSummary();
    } catch (error) {
        systemMessage.pending = false;
        systemMessage.error = true;
        systemMessage.content = `Не удалось саммаризировать контекст: ${error.message}`;
    } finally {
        state.summarizing = false;
        updateMessageView(systemMessage);
        persistMessages();
        updateTokenStats();
    }
}

function buildPayload(prompt, model, images) {
    const s = state.settings;
    const payload = {
        prompt,
        model,
        stream: true,
        thinking: s.thinking,
        reasoning_effort: s.reasoning_effort,
    };
    for (const key of numberFields) {
        const raw = s[key];
        if (raw === '' || raw == null) {
            continue;
        }
        const value = Number(raw);
        if (Number.isNaN(value)) {
            continue;
        }
        if (key === 'top_p' && value <= 0) {
            continue;
        }
        payload[key] = value;
    }
    const stop = parseStop(s.stop);
    if (stop.length > 0) {
        payload.stop = stop;
    }
    const systemPrompt = (s.systemPrompt || '').trim();
    if (systemPrompt) {
        payload.system_prompt = systemPrompt;
    }
    if (images && images.length > 0) {
        payload.attachments = images;
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
    const hasFiles = attachments.hasAny();
    if ((!text && !hasFiles) || state.streaming) {
        return;
    }
    if (!state.settings.model) {
        showToast('Выберите модель', true);
        return;
    }

    const topP = Number(state.settings.top_p);
    if (state.settings.top_p !== '' && !Number.isNaN(topP) && topP <= 0) {
        showToast('Top P должен быть больше 0', true);
        return;
    }

    const images = attachments.getImagePayload();
    let model = state.settings.model;
    if (images.length > 0) {
        const visionModel = state.models.find((item) => item.vision);
        if (!visionModel) {
            showToast('Нет доступной vision-модели для изображений', true);
            return;
        }
        model = visionModel.alias;
    }

    const textContext = attachments.getTextContext();
    const fullText = [text, textContext].filter(Boolean).join('\n\n');
    const prompt = buildPrompt(state.messages, fullText);
    const previews = attachments.getPreviews();

    const userMessage = { id: newId(), role: 'user', content: text, attachments: previews };
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
    attachments.clear();
    setStreaming(true);

    const payload = buildPayload(prompt, model, images);
    try {
        await streamCompletion(payload, {
            onChunk: (data) => {
                assistantMessage.content += data?.content || '';
                assistantMessage.reasoningContent += data?.reasoningContent || '';
                updateMessageView(assistantMessage, true);
            },
            onUsage: (data) => {
                assistantMessage.usage = data;
                updateMessageView(assistantMessage, true);
                updateTokenStats();
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
    } catch (error) {
        assistantMessage.error = true;
        assistantMessage.content = error.message || String(error);
        showToast(`Ошибка: ${error.message}`, true);
    } finally {
        assistantMessage.pending = false;
        updateMessageView(assistantMessage);
        persistMessages();
        updateTokenStats();
        setStreaming(false);
        maybeSummarize();
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
    dom.context.addEventListener('change', () => {
        state.settings.context = dom.context.checked;
        persistSettings();
        updateSummaryControls();
    });
    dom.summarize.addEventListener('change', () => {
        state.settings.summarize = dom.summarize.checked;
        persistSettings();
        updateSummaryControls();
    });
    dom.summaryInterval.addEventListener('input', () => {
        const value = Math.floor(Number(dom.summaryInterval.value));
        if (Number.isFinite(value) && value >= 1) {
            state.settings.summaryInterval = value;
            persistSettings();
        }
    });
    dom.summaryInterval.addEventListener('change', () => {
        const value = summaryInterval();
        dom.summaryInterval.value = value;
        state.settings.summaryInterval = value;
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
    attachments = initAttachments({
        onError: (message) => showToast(message, true),
    });

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
        resetSummary();
        renderMessages();
        attachments.clear();
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

    dom.logout.addEventListener('click', async () => {
        await logout();
        window.location.replace('login.html');
    });
}

async function requireSession() {
    try {
        const user = await me();
        if (!user) {
            window.location.replace('login.html');
            return null;
        }
        dom.user.textContent = user.username;
        return user;
    } catch {
        window.location.replace('login.html');
        return null;
    }
}

async function init() {
    loadState();
    applyTheme();
    applySettingsToInputs();
    bindSettings();
    bindEvents();
    renderMessages();
    autoResize();

    const user = await requireSession();
    if (!user) {
        return;
    }

    loadModels();
    dom.prompt.focus();
}

init();
