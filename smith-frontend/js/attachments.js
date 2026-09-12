const MAX_FILE_SIZE = 20 * 1024 * 1024;
const MAX_ATTACHMENTS = 10;
const MAX_TEXT_LENGTH = 50000;

const TEXT_EXTENSIONS = [
    'txt', 'md', 'markdown', 'csv', 'json', 'log', 'yaml', 'yml',
    'xml', 'html', 'css', 'js', 'ts', 'java', 'kt', 'py', 'sql', 'sh',
];

const items = [];

let container = null;
let fileInput = null;
let composer = null;
let overlay = null;
let onError = () => {};
let dragDepth = 0;

function isTextFile(file) {
    if (file.type.startsWith('text/')) {
        return true;
    }
    if (file.type === 'application/json' || file.type === 'application/xml') {
        return true;
    }
    const ext = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : '';
    return TEXT_EXTENSIONS.includes(ext);
}

function classify(file) {
    if (file.type.startsWith('image/')) {
        return 'image';
    }
    if (isTextFile(file)) {
        return 'text';
    }
    return 'unsupported';
}

function formatSize(bytes) {
    if (bytes < 1024) {
        return `${bytes} Б`;
    }
    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} КБ`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`;
}

function readAsDataUrl(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(reader.error);
        reader.readAsDataURL(file);
    });
}

function readAsText(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(reader.error);
        reader.readAsText(file);
    });
}

async function toItem(file) {
    const kind = classify(file);
    if (kind === 'unsupported') {
        onError(`Тип файла не поддерживается: ${file.name}`);
        return null;
    }
    if (file.size > MAX_FILE_SIZE) {
        onError(`Файл слишком большой (макс. 20 МБ): ${file.name}`);
        return null;
    }
    if (kind === 'image') {
        const dataUrl = await readAsDataUrl(file);
        const comma = dataUrl.indexOf(',');
        return {
            id: crypto.randomUUID ? crypto.randomUUID() : `att-${Date.now()}-${Math.random()}`,
            kind,
            name: file.name,
            mimeType: file.type || 'image/png',
            size: file.size,
            dataUrl,
            base64: dataUrl.slice(comma + 1),
        };
    }
    let text = await readAsText(file);
    let truncated = false;
    if (text.length > MAX_TEXT_LENGTH) {
        text = text.slice(0, MAX_TEXT_LENGTH);
        truncated = true;
    }
    return {
        id: crypto.randomUUID ? crypto.randomUUID() : `att-${Date.now()}-${Math.random()}`,
        kind,
        name: file.name,
        mimeType: file.type || 'text/plain',
        size: file.size,
        text,
        truncated,
    };
}

function render() {
    if (!container) {
        return;
    }
    container.replaceChildren();
    container.hidden = items.length === 0;
    for (const item of items) {
        const chip = document.createElement('div');
        chip.className = 'attachment';

        if (item.kind === 'image') {
            const thumb = document.createElement('img');
            thumb.className = 'attachment__thumb';
            thumb.src = item.dataUrl;
            thumb.alt = item.name;
            chip.append(thumb);
        } else {
            const badge = document.createElement('span');
            badge.className = 'attachment__badge';
            badge.textContent = 'TXT';
            chip.append(badge);
        }

        const meta = document.createElement('span');
        meta.className = 'attachment__meta';
        const name = document.createElement('span');
        name.className = 'attachment__name';
        name.textContent = item.name;
        name.title = item.name;
        const size = document.createElement('span');
        size.className = 'attachment__size';
        size.textContent = formatSize(item.size);
        meta.append(name, size);
        chip.append(meta);

        const remove = document.createElement('button');
        remove.type = 'button';
        remove.className = 'attachment__remove';
        remove.title = 'Убрать';
        remove.setAttribute('aria-label', `Убрать ${item.name}`);
        remove.textContent = '×';
        remove.addEventListener('click', () => {
            const index = items.findIndex((entry) => entry.id === item.id);
            if (index !== -1) {
                items.splice(index, 1);
                render();
            }
        });
        chip.append(remove);

        container.append(chip);
    }
}

async function addFiles(fileList) {
    const files = Array.from(fileList || []);
    if (files.length === 0) {
        return;
    }
    const free = MAX_ATTACHMENTS - items.length;
    if (free <= 0) {
        onError(`Можно прикрепить не более ${MAX_ATTACHMENTS} файлов`);
        return;
    }
    const accepted = files.slice(0, free);
    if (files.length > free) {
        onError(`Можно прикрепить не более ${MAX_ATTACHMENTS} файлов`);
    }
    for (const file of accepted) {
        try {
            const item = await toItem(file);
            if (item) {
                items.push(item);
            }
        } catch {
            onError(`Не удалось прочитать файл: ${file.name}`);
        }
    }
    render();
}

function clear() {
    items.length = 0;
    render();
}

function hasImages() {
    return items.some((item) => item.kind === 'image');
}

function hasAny() {
    return items.length > 0;
}

function getImagePayload() {
    return items
        .filter((item) => item.kind === 'image')
        .map((item) => ({ name: item.name, mime_type: item.mimeType, data: item.base64 }));
}

function getTextContext() {
    return items
        .filter((item) => item.kind === 'text')
        .map((item) => `[файл: ${item.name}]\n${item.text}${item.truncated ? '\n…[файл обрезан]' : ''}`)
        .join('\n\n');
}

function getPreviews() {
    return items.map((item) => ({
        kind: item.kind,
        name: item.name,
        size: item.size,
    }));
}

function showOverlay() {
    overlay?.classList.add('drop-overlay--visible');
    composer?.classList.add('composer--dragover');
}

function hideOverlay() {
    overlay?.classList.remove('drop-overlay--visible');
    composer?.classList.remove('composer--dragover');
}

function dragHasFiles(event) {
    return event.dataTransfer
        && Array.from(event.dataTransfer.types || []).includes('Files');
}

function bindDragAndDrop() {
    document.addEventListener('dragenter', (event) => {
        if (!dragHasFiles(event)) {
            return;
        }
        event.preventDefault();
        dragDepth += 1;
        showOverlay();
    });
    document.addEventListener('dragover', (event) => {
        if (!dragHasFiles(event)) {
            return;
        }
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
    });
    document.addEventListener('dragleave', (event) => {
        if (!dragHasFiles(event)) {
            return;
        }
        dragDepth = Math.max(0, dragDepth - 1);
        if (dragDepth === 0) {
            hideOverlay();
        }
    });
    document.addEventListener('drop', (event) => {
        if (!dragHasFiles(event)) {
            return;
        }
        event.preventDefault();
        dragDepth = 0;
        hideOverlay();
        addFiles(event.dataTransfer.files);
    });
}

export function initAttachments(options = {}) {
    container = document.getElementById('attachments');
    fileInput = document.getElementById('file-input');
    composer = document.getElementById('composer');
    overlay = document.getElementById('drop-overlay');
    onError = options.onError || onError;

    const attach = document.getElementById('attach');
    attach?.addEventListener('click', () => fileInput?.click());
    fileInput?.addEventListener('change', () => {
        addFiles(fileInput.files);
        fileInput.value = '';
    });

    bindDragAndDrop();
    render();

    return {
        addFiles,
        clear,
        hasImages,
        hasAny,
        getImagePayload,
        getTextContext,
        getPreviews,
    };
}
