for (const field of document.querySelectorAll("[data-otp-input]")) {
    const input = field.querySelector("[data-otp-value]");
    const slots = field.querySelector("[data-otp-slots]");
    const cellClass = field.dataset.otpCellClass?.trim();
    let digits = Number(field.dataset.otpDigits);
    let cells = [];
    let pointerType;
    let selectionAnchor = null;

    if (!input || !slots || !cellClass || !Number.isInteger(digits) || digits < 1) {
        continue;
    }

    const normalize = (value) => value.replace(/[^0-9]/g, "");

    function render() {
        const start = input.selectionStart ?? input.value.length;
        const end = input.selectionEnd ?? start;
        const active = Math.min(start, digits - 1);
        cells.forEach((cell, index) => {
            cell.textContent = input.value[index] ?? "";
            cell.toggleAttribute("data-active", index === active);
            cell.toggleAttribute("data-selected", index >= start && index < end);
        });
    }

    function setDigits(nextDigits) {
        if (!Number.isInteger(nextDigits) || nextDigits < 1) {
            return;
        }
        if (nextDigits === digits && cells.length) {
            render();
            return;
        }
        if (nextDigits !== digits) {
            input.value = "";
        }
        digits = nextDigits;
        field.dataset.otpDigits = String(digits);
        input.maxLength = digits;
        cells = Array.from({ length: digits }, () => {
            const cell = document.createElement("span");
            cell.className = cellClass;
            return cell;
        });
        slots.replaceChildren(...cells);
        render();
    }

    function onInput(event) {
        if (event.isComposing) {
            return;
        }
        const value = input.value;
        const start = input.selectionStart ?? value.length;
        const end = input.selectionEnd ?? start;
        const normalized = normalize(value).slice(0, digits);
        if (value !== normalized) {
            input.value = normalized;
            input.setSelectionRange(normalize(value.slice(0, start)).length, normalize(value.slice(0, end)).length);
        }
        const position = input.selectionStart;
        if (event.inputType?.startsWith("insert") && position === input.selectionEnd && position < input.value.length) {
            input.setSelectionRange(position, position + 1);
        }
        render();
    }

    input.addEventListener("input", onInput);
    input.addEventListener("compositionend", onInput);
    input.addEventListener("paste", (event) => {
        const text = event.clipboardData?.getData("text/plain");
        if (text === undefined) {
            return;
        }
        event.preventDefault();
        const code = normalize(text);
        if (!code) {
            return;
        }
        if (code.length >= digits) {
            input.value = code.slice(0, digits);
            input.setSelectionRange(digits, digits);
        } else {
            const start = input.selectionStart;
            const end = Math.max(input.selectionEnd, start + code.length);
            input.setRangeText(code, start, Math.min(end, input.value.length), "end");
        }
        input.dispatchEvent(new InputEvent("input", { bubbles: true, inputType: "insertFromPaste", data: code }));
    });

    function indexAt(clientX) {
        const index = cells.findIndex((cell) => clientX < cell.getBoundingClientRect().right);
        return Math.min(index < 0 ? digits - 1 : index, input.value.length);
    }

    function selectCell(index, anchor = index) {
        const start = Math.min(anchor, index);
        const end = Math.min(Math.max(anchor, index) + 1, input.value.length);
        input.setSelectionRange(start, end, index < anchor ? "backward" : "forward");
        render();
    }

    input.addEventListener("pointerdown", (event) => {
        pointerType = event.pointerType;
        if (pointerType !== "mouse" || event.button !== 0) {
            return;
        }
        event.preventDefault();
        input.focus();
        const index = indexAt(event.clientX);
        selectionAnchor = event.shiftKey
            ? (input.selectionDirection === "backward" ? Math.max(0, input.selectionEnd - 1) : input.selectionStart)
            : index;
        selectCell(index, selectionAnchor);
        input.setPointerCapture(event.pointerId);
    });
    input.addEventListener("pointermove", (event) => {
        if (selectionAnchor !== null) {
            selectCell(indexAt(event.clientX), selectionAnchor);
        }
    });
    input.addEventListener("lostpointercapture", () => {
        selectionAnchor = null;
    });

    input.addEventListener("click", (event) => {
        if (event.detail > 1) {
            input.select();
            return;
        }
        if (pointerType === "mouse" || event.detail === 0 || event.shiftKey) {
            return;
        }
        selectCell(indexAt(event.clientX));
    });
    input.addEventListener("keydown", (event) => {
        if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) {
            return;
        }
        let position = input.selectionStart;
        if (event.key === "ArrowLeft") {
            position = Math.max(0, position - 1);
        } else if (event.key === "ArrowRight") {
            position = Math.min(input.value.length, position + 1);
        } else if (event.key === "Home") {
            position = 0;
        } else {
            return;
        }
        event.preventDefault();
        selectCell(position);
    });
    for (const event of ["focus", "blur", "keyup", "select"]) {
        input.addEventListener(event, render);
    }
    document.addEventListener("selectionchange", () => {
        if (document.activeElement === input) {
            render();
        }
    });
    const credentials = Array.from(input.form?.elements ?? [])
        .filter((element) => element.name === field.dataset.otpCredentialField && element.type === "radio");
    const updateCredential = () => {
        const selected = credentials.find((credential) => credential.checked);
        if (selected) {
            setDigits(Number(selected.dataset.otpDigits));
        } else {
            render();
        }
    };
    for (const credential of credentials) {
        credential.addEventListener("change", updateCredential);
    }
    input.form?.addEventListener("reset", () => queueMicrotask(updateCredential));
    window.addEventListener("pageshow", updateCredential);

    setDigits(digits);
    updateCredential();
    slots.hidden = false;
    field.setAttribute("data-otp-enhanced", "");
}
