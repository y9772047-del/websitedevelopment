let currentThemeStorageKey = null;

const BUILT_IN_THEMES = [
    {
        key: "theme-pink",
        name: "浅粉暖色",
        desc: "适合温柔清爽的学习风格",
        primary: "#bd6b7a",
        hover: "#a95767",
        background: "#fffaf8",
        border: "#efd1d7"
    },
    {
        key: "theme-blue",
        name: "清新蓝色",
        desc: "适合简洁理性的学习风格",
        primary: "#5b8def",
        hover: "#3f74df",
        background: "#f5f8ff",
        border: "#d7e4ff"
    },
    {
        key: "theme-green",
        name: "抹茶绿色",
        desc: "适合护眼安静的学习风格",
        primary: "#6fa878",
        hover: "#5b9164",
        background: "#f7fbf6",
        border: "#d7ead8"
    },
    {
        key: "theme-cream",
        name: "奶油米白",
        desc: "适合简洁柔和的学习风格",
        primary: "#c69c6d",
        hover: "#ad8455",
        background: "#fffaf2",
        border: "#eddcc4"
    },
    {
        key: "theme-dark",
        name: "夜间模式",
        desc: "适合夜间学习",
        primary: "#8b7cf6",
        hover: "#7767e6",
        background: "#1f1f2e",
        border: "#45415f"
    }
];

function getSavedThemeKey() {
    if (!currentThemeStorageKey) {
        return "theme-pink";
    }
    return localStorage.getItem(currentThemeStorageKey) || "theme-pink";
}

function findTheme(themeKey) {
    return BUILT_IN_THEMES.find(theme => theme.key === themeKey) || BUILT_IN_THEMES[0];
}

function applyTheme(themeKey) {
    document.body.classList.remove(...BUILT_IN_THEMES.map(theme => theme.key));
    document.body.classList.add(themeKey);
    updateCurrentThemeArea(themeKey);
}

function previewTheme(themeKey) {
    applyTheme(themeKey);
    showThemeToast(`正在预览：${findTheme(themeKey).name}`);
}

function useTheme(themeKey) {
    if (currentThemeStorageKey) {
        localStorage.setItem(currentThemeStorageKey, themeKey);
    }
    applyTheme(themeKey);
    renderThemeCards();
    showThemeToast(`已切换为：${findTheme(themeKey).name}`);
}

function buildThemeStorageKey(user) {
    if (!user) return null;
    if (user.id !== null && user.id !== undefined) {
        return `theme_${user.id}`;
    }
    if (user.username) {
        return `theme_${user.username}`;
    }
    return null;
}

async function initUserTheme() {
    try {
        const response = await fetch("../api/auth/current");
        if (response.ok) {
            const result = await response.json();
            currentThemeStorageKey = buildThemeStorageKey(result.data);
        }
    } catch (error) {
        currentThemeStorageKey = null;
    }
    applyTheme(getSavedThemeKey());
    renderThemeCards();
}

function updateCurrentThemeArea(themeKey) {
    const theme = findTheme(themeKey);
    const badge = document.getElementById("currentThemeBadge");
    const desc = document.getElementById("currentThemeDesc");
    if (badge) badge.textContent = theme.name;
    if (desc) desc.textContent = theme.desc;
}

function renderThemeCards() {
    const list = document.getElementById("themeCardList");
    if (!list) return;
    const savedTheme = getSavedThemeKey();
    list.innerHTML = BUILT_IN_THEMES.map(theme => `
        <div class="col-md-6 col-xl-4">
            <div class="content-card theme-card ${theme.key === savedTheme ? "active-theme" : ""}">
                <div class="card-title-row">
                    <h2>${theme.name}</h2>
                    ${theme.key === savedTheme ? '<span class="badge soft-badge">使用中</span>' : ""}
                </div>
                <p class="muted-text">${theme.desc}</p>
                <div class="theme-swatch">
                    <span style="background:${theme.primary}"></span>
                    <span style="background:${theme.hover}"></span>
                    <span style="background:${theme.background}"></span>
                    <span style="background:${theme.border}"></span>
                </div>
                <div class="d-flex gap-2">
                    <button class="btn btn-outline-primary btn-sm" type="button" onclick="previewTheme('${theme.key}')">预览</button>
                    <button class="btn btn-primary btn-sm" type="button" onclick="useTheme('${theme.key}')">立即使用</button>
                </div>
            </div>
        </div>
    `).join("");
}

function showThemeToast(message) {
    let toast = document.getElementById("themeToast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "themeToast";
        toast.className = "position-fixed bottom-0 end-0 m-4 px-3 py-2 rounded-3 shadow";
        toast.style.zIndex = "1080";
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.style.background = "var(--primary)";
    toast.style.color = "#fff";
    toast.classList.remove("d-none");
    clearTimeout(window.themeToastTimer);
    window.themeToastTimer = setTimeout(() => toast.classList.add("d-none"), 1800);
}

document.addEventListener("DOMContentLoaded", initUserTheme);
