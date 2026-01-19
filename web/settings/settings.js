const ws = new WebSocket("ws://localhost:8080/ws/settings");

const container = document.createElement("div");
document.body.appendChild(container);

let currentModel = null;
const controlMap = {};

ws.onopen = () => {
    ws.send("request");
};

ws.onmessage = e => {
    console.log("from server:", e.data);
    updateUI(e.data);
};

function reportValue(model, key, value) {
    let msg = {
        model: model,
        key: key,
        value: value
    };

    ws.send(JSON.stringify(msg));
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