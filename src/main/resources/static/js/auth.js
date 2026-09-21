function formToObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

async function postJson(url, data) {
    const response = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(data)
    });
    return response.json();
}

function showAuthMessage(message) {
    document.getElementById("authMessage").textContent = message;
}

document.querySelectorAll("#loginTabs .nav-link").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll("#loginTabs .nav-link").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
        document.querySelectorAll(".auth-form").forEach(form => form.classList.add("d-none"));
        document.getElementById(button.dataset.target).classList.remove("d-none");
    });
});

document.getElementById("loginForm").addEventListener("submit", async event => {
    event.preventDefault();
    const result = await postJson("api/auth/login", formToObject(event.target));
    showAuthMessage(result.message);
    if (result.success) {
        window.location.href = result.data && result.data.role === "admin" ? "pages/admin.html" : "pages/app.html";
    }
});

document.getElementById("registerForm").addEventListener("submit", async event => {
    event.preventDefault();
    const result = await postJson("api/auth/register", formToObject(event.target));
    showAuthMessage(result.message);
    if (result.success) {
        document.querySelector('[data-target="loginForm"]').click();
    }
});
