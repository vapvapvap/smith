const ESCAPES = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
};

function escapeHtml(text) {
    return String(text).replace(/[&<>"']/g, (char) => ESCAPES[char]);
}

export function renderMarkdown(text) {
    if (!text) {
        return '';
    }
    let html = escapeHtml(text);
    html = html.replace(/```[^\n]*\n?([\s\S]*?)```/g, (match, code) =>
        `<pre><code>${code.replace(/\n$/, '')}</code></pre>`);
    html = html.replace(/`([^`\n]+)`/g, '<code>$1</code>');
    return html;
}

function formatTokens(usage) {
    if (!usage) {
        return '';
    }
    const { promptTokens, completionTokens, totalTokens } = usage;
    if (promptTokens == null && completionTokens == null && totalTokens == null) {
        return '';
    }
    return `токены: ${promptTokens ?? '—'} + ${completionTokens ?? '—'} = ${totalTokens ?? '—'}`;
}

export class MessageView {
    constructor(message) {
        this.el = document.createElement('div');
        this.el.dataset.id = message.id;

        const avatar = document.createElement('div');
        avatar.className = 'msg__avatar';

        this.content = document.createElement('div');
        this.content.className = 'msg__content';

        this.reasoning = document.createElement('details');
        this.reasoning.className = 'reasoning';
        this.reasoning.hidden = true;
        const summary = document.createElement('summary');
        summary.textContent = 'Рассуждения';
        this.reasoningBody = document.createElement('div');
        this.reasoningBody.className = 'reasoning__body';
        this.reasoning.append(summary, this.reasoningBody);

        this.attachments = document.createElement('div');
        this.attachments.className = 'msg__attachments';

        this.bubble = document.createElement('div');
        this.bubble.className = 'msg__bubble';

        this.meta = document.createElement('div');
        this.meta.className = 'msg__meta';

        this.content.append(this.reasoning, this.attachments, this.bubble, this.meta);
        this.el.append(avatar, this.content);

        this.update(message);
    }

    update(message) {
        const isUser = message.role === 'user';
        this.el.className = `msg msg--${message.role}${message.error ? ' msg--error' : ''}`;
        this.el.querySelector('.msg__avatar').textContent = isUser ? 'U' : 'S';

        if (message.reasoningContent) {
            this.reasoning.hidden = false;
            this.reasoningBody.textContent = message.reasoningContent;
        } else {
            this.reasoning.hidden = true;
        }

        const files = message.attachments || [];
        this.attachments.replaceChildren();
        this.attachments.hidden = files.length === 0;
        for (const file of files) {
            const chip = document.createElement('span');
            chip.className = 'msg__attachment';
            const badge = document.createElement('span');
            badge.className = 'msg__attachment-badge';
            badge.textContent = file.kind === 'image' ? 'IMG' : 'TXT';
            const name = document.createElement('span');
            name.className = 'msg__attachment-name';
            name.textContent = file.name;
            chip.append(badge, name);
            this.attachments.append(chip);
        }

        if (message.pending && !message.content && !message.reasoningContent) {
            this.bubble.innerHTML = '<span class="typing"><span></span><span></span><span></span></span>';
        } else if (message.error) {
            this.bubble.textContent = message.content || 'Произошла ошибка';
        } else {
            this.bubble.innerHTML = renderMarkdown(message.content);
        }

        if (isUser) {
            this.meta.textContent = '';
            return;
        }

        const parts = [];
        if (message.finishReason) {
            parts.push(`завершено: ${message.finishReason}`);
        }
        const tokens = formatTokens(message.usage);
        if (tokens) {
            parts.push(tokens);
        }
        this.meta.textContent = parts.join(' · ');
    }
}
