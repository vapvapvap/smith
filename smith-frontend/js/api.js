import { apiUrl, authUrl } from './config.js';

function redirectToLogin() {
    if (!window.location.pathname.endsWith('login.html')) {
        window.location.replace('login.html');
    }
}

async function toError(response) {
    let message = `HTTP ${response.status}`;
    try {
        const body = await response.json();
        if (body && body.message) {
            message = body.message;
        } else if (body && body.error) {
            message = body.error;
        }
    } catch {
        /* non-JSON body */
    }
    const error = new Error(message);
    error.status = response.status;
    return error;
}

function ensureAuthorized(response) {
    if (response.status === 401) {
        redirectToLogin();
        throw new Error('Требуется авторизация');
    }
}

export async function login(username, password) {
    const response = await fetch(authUrl('/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ username, password }),
        credentials: 'include',
    });
    if (!response.ok) {
        throw await toError(response);
    }
    return response.json();
}

export async function logout() {
    try {
        await fetch(authUrl('/logout'), {
            method: 'POST',
            credentials: 'include',
        });
    } catch {
        /* ignore network errors on logout */
    }
}

export async function me() {
    const response = await fetch(authUrl('/me'), { credentials: 'include' });
    if (response.status === 401) {
        return null;
    }
    if (!response.ok) {
        throw await toError(response);
    }
    return response.json();
}

export async function fetchModels() {
    const response = await fetch(apiUrl('/models'), { credentials: 'include' });
    ensureAuthorized(response);
    if (!response.ok) {
        throw await toError(response);
    }
    return response.json();
}

export async function complete(payload) {
    const response = await fetch(apiUrl('/chat/completions'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        credentials: 'include',
    });
    ensureAuthorized(response);
    if (!response.ok) {
        throw await toError(response);
    }
    return response.json();
}

function parseEvent(raw) {
    let event = 'message';
    const dataLines = [];

    for (const line of raw.split('\n')) {
        if (line.startsWith(':')) {
            continue;
        }
        const colon = line.indexOf(':');
        const field = colon === -1 ? line : line.slice(0, colon);
        let value = colon === -1 ? '' : line.slice(colon + 1);
        if (value.startsWith(' ')) {
            value = value.slice(1);
        }
        if (field === 'event') {
            event = value;
        } else if (field === 'data') {
            dataLines.push(value);
        }
    }

    if (dataLines.length === 0) {
        return null;
    }

    const dataString = dataLines.join('\n');
    let data = dataString;
    try {
        data = JSON.parse(dataString);
    } catch {
        /* keep raw string */
    }
    return { event, data };
}

function dispatch({ event, data }, handlers) {
    switch (event) {
        case 'chunk':
            handlers.onChunk?.(data);
            break;
        case 'usage':
            handlers.onUsage?.(data);
            break;
        case 'done':
            handlers.onDone?.(data);
            break;
        case 'error':
            handlers.onError?.(data);
            break;
        default:
            break;
    }
}

export async function streamCompletion(payload, handlers, signal) {
    const response = await fetch(apiUrl('/chat/completions/stream'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal,
        credentials: 'include',
    });
    ensureAuthorized(response);
    if (!response.ok) {
        throw await toError(response);
    }
    if (!response.body) {
        throw new Error('Стриминг не поддерживается браузером');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let failed = false;

    try {
        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });

            let boundary = buffer.indexOf('\n\n');
            while (boundary !== -1) {
                const raw = buffer.slice(0, boundary);
                buffer = buffer.slice(boundary + 2);
                const parsed = parseEvent(raw);
                if (parsed) {
                    dispatch(parsed, handlers);
                    if (parsed.event === 'error') {
                        failed = true;
                    }
                }
                boundary = buffer.indexOf('\n\n');
            }

            if (failed) {
                break;
            }
        }
    } catch (error) {
        if (!failed && error.name !== 'AbortError') {
            throw error;
        }
    } finally {
        try {
            await reader.cancel();
        } catch {
            /* reader already closed */
        }
    }
}
