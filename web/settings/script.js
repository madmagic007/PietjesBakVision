let settingsWs;
let videoWs;

let settingsReconnectTimer;
let videoReconnectTimer;

const slidersDiv = document.getElementById("sliders")
const container = document.createElement("div");
slidersDiv.appendChild(container);

let currentModel = null;
const controlMap = {};

function settingsConnect() {
    if (settingsWs && (settingsWs.readyState === WebSocket.OPEN || settingsWs.readyState === WebSocket.CONNECTING)) {
        return;
    }

    settingsWs = new WebSocket("ws://" + window.location.host + "/ws/settings");

    settingsWs.onopen = () => {
        settingsWs.send("request");

        if (settingsReconnectTimer) {
            clearTimeout(settingsReconnectTimer);
            settingsReconnectTimer = null;
        }
    };

    settingsWs.onclose = () => {
        if (!settingsReconnectTimer) {
            settingsReconnectTimer = setTimeout(settingsConnect, 1000);
        }
    };

    settingsWs.onmessage = e => {
        if (e.data == "pong") return;
        console.log("from server:", e.data);
        updateUI(e.data);
    };
}
settingsConnect();

setInterval(() => {
    if (settingsWs.readyState === WebSocket.OPEN) {
        settingsWs.send("ping");
    }
}, 5000);

function reportValue(model, key, value) {
    let msg = {
        type: "update",
        model: model,
        key: key,
        value: value
    };

    settingsWs.send(JSON.stringify(msg));
}

function sanitizeValue(val, min, max) {
    let num = Number(val);
    if (isNaN(num)) num = min;
    if (num < min) num = min;
    if (num > max) num = max;
    return num;
}

function updateUI(message) {
    const data = typeof message === "string" ? JSON.parse(message) : message;
    const activeModel = data.models?.active || currentModel;
    const params = data.params || {};
    const rebuild = activeModel !== currentModel;
    currentModel = activeModel;

    if (rebuild) {
        container.innerHTML = "";
        Object.keys(controlMap).forEach(k => delete controlMap[k]);
    }

    const checkboxes = [];
    const sliders = [];

    for (const key in params) {
        const setting = params[key];
        if (!("min" in setting && "max" in setting)) checkboxes.push([key, setting]);
        else sliders.push([key, setting]);
    }

    function createControl(key, setting, type) {
        const wrapper = document.createElement("div");
        wrapper.className = "control-wrapper";

        const label = document.createElement("label");
        label.textContent = key;
        label.className = "control-label";
        wrapper.appendChild(label);

        let inputEl;

        if (type === "checkbox") {
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.checked = setting.value === 1;
            checkbox.className = "control-checkbox";
            checkbox.onchange = () => reportValue(currentModel, key, checkbox.checked ? 1 : 0);
            wrapper.appendChild(checkbox);
            inputEl = checkbox;
        } else {
            const minLabel = document.createElement("span");
            minLabel.textContent = setting.min;
            minLabel.className = "control-min";
            wrapper.appendChild(minLabel);

            const slider = document.createElement("input");
            slider.type = "range";
            slider.min = setting.min;
            slider.max = setting.max;
            slider.value = setting.value;
            slider.className = "control-slider";
            slider.oninput = () => {
                valueInput.value = slider.value;
            };
            slider.onchange = () => reportValue(currentModel, key, Number(slider.value));
            wrapper.appendChild(slider);

            const maxLabel = document.createElement("span");
            maxLabel.textContent = setting.max;
            maxLabel.className = "control-max";
            wrapper.appendChild(maxLabel);

            const valueInput = document.createElement("input");
            valueInput.type = "number";
            valueInput.value = setting.value;
            valueInput.min = setting.min;
            valueInput.max = setting.max;
            valueInput.className = "control-value";
            valueInput.onchange = () => {
                const sanitized = sanitizeValue(valueInput.value, setting.min, setting.max);
                valueInput.value = sanitized;
                slider.value = sanitized;
                reportValue(currentModel, key, sanitized);
            };
            wrapper.appendChild(valueInput);

            inputEl = slider;
        }

        container.appendChild(wrapper);
        controlMap[key] = { wrapper, inputEl };
    }

    for (const [key, setting] of checkboxes) {
        if (rebuild || !controlMap[key]) createControl(key, setting, "checkbox");
        else controlMap[key].inputEl.checked = setting.value === 1;
    }

    for (const [key, setting] of sliders) {
        if (rebuild || !controlMap[key]) createControl(key, setting, "slider");
        else {
            const c = controlMap[key];
            c.inputEl.value = setting.value;
            const valueInput = c.wrapper.querySelector('input[type="number"]');
            if (valueInput) valueInput.value = setting.value;
        }
    }
}

const canvasesContainer = document.getElementById("mats");
const canvases = [];

let availableFrame = null;
let rendering = false;

function videoConnect() {
    videoWs = new WebSocket("ws://" + window.location.host + "/ws/video");
    videoWs.binaryType = "arraybuffer";

    videoWs.onmessage = e => {
        if (!(e.data instanceof ArrayBuffer)) return;

        availableFrame = e.data;
        if (!rendering) renderFrames();
    };

    videoWs.onopen = e => {
        if (videoReconnectTimer) {
            clearTimeout(videoReconnectTimer);
            videoReconnectTimer = null;
        }
    }

    videoWs.onclose = () => {
        if (!videoReconnectTimer) {
            videoReconnectTimer = setTimeout(videoConnect, 1000);
        }
    };
}
videoConnect();

setInterval(() => {
    if (videoWs.readyState === WebSocket.OPEN) {
        videoWs.send("ping");
    }
}, 5000);

function renderFrames() {
    const e = availableFrame;
    if (!e) return;

    availableFrame = null;
    rendering = true;

    const dv = new DataView(e);
    let offset = 0;

    const count = dv.getInt32(offset);
    offset += 4;

    for (let i = 0; i < count; i++) {
        const imgLen = dv.getInt32(offset);
        offset += 4;

        const imgBytes = new Uint8Array(e, offset, imgLen);
        offset += imgLen;

        const tDecodeStart = performance.now();
        createImageBitmap(new Blob([imgBytes], { type: "image/jpeg" })).then(bitmap => {
            const tDecodeEnd = performance.now();

            let ctx = canvases[i];
            if (!ctx) {
                const canvas = document.createElement("canvas");
                container.appendChild(canvas);
                ctx = canvas.getContext("2d");
                canvases[i] = ctx;
            }

            const tDrawStart = performance.now();
            ctx.canvas.width = bitmap.width;
            ctx.canvas.height = bitmap.height;
            ctx.drawImage(bitmap, 0, 0);
            const tDrawEnd = performance.now();
        });
    }

    if (availableFrame) renderFrames();
    rendering = false;
};