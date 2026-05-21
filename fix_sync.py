import re
with open('frontend/sync.js', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Imports
content = re.sub(r"import \{ getDemoCredentials.*?\n", "", content)
content = re.sub(r"import \{ emitOnboardingEvent.*?\n", "", content)

# 2. demoFillLoginBtn definition
content = re.sub(r"    const demoFillLoginBtn = document\.getElementById\('demoFillLoginBtn'\);\n", "", content)

# 3. fillDemoCredentials
content = re.sub(r"    const fillDemoCredentials = \(\) => \{[\s\S]*?\};\n\n", "", content)

# 4. openLoginModal
content = re.sub(r"        if \(demoFillLoginBtn\) \{[\s\S]*?loadSchoolCaptcha\(\);\n        \}", "        captchaInput.value = '';\n        loadSchoolCaptcha();", content)
content = re.sub(r"        emitOnboardingEvent\(ONBOARDING_EVENTS.LOGIN_MODAL_OPEN\);\n", "", content)

# 5. syncBtn.addEventListener
content = re.sub(r"    syncBtn.addEventListener\('click', async \(\) => \{\n        if \(isDemoModeEnabled\(\)\) \{\n            openLoginModal\(\);\n            return;\n        \}\n        openSelectExamModal\(\);\n    \}\);\n", "    syncBtn.addEventListener('click', async () => {\n        openSelectExamModal();\n    });\n", content)

# 6. handleLogin demo checks
content = re.sub(r"        if \(!isDemoModeEnabled\(\) && !captchaCode\) \{", "        if (!captchaCode) {", content)
content = re.sub(r"        if \(isDemoModeEnabled\(\)\) \{[\s\S]*?        \}\n\n        // Turnstile", "        // Turnstile", content)
content = re.sub(r"                emitOnboardingEvent\(ONBOARDING_EVENTS.LOGIN_SUCCESS\);\n", "", content)

# 7. demoFillLoginBtn event listener
content = re.sub(r"    if \(demoFillLoginBtn\) \{[\s\S]*?\}\n\n", "", content)

# 8. openSelectExamModal demo
content = re.sub(r"        if \(isDemoModeEnabled\(\)\) \{[\s\S]*?return;\n        \}\n\n", "", content)
content = re.sub(r"            emitOnboardingEvent\(ONBOARDING_EVENTS.SELECT_MODAL_OPEN\);\n", "", content)

# 9. confirmFetch.addEventListener demo
content = re.sub(r"        if \(isDemoModeEnabled\(\)\) \{[\s\S]*?return;\n        \}\n\n", "", content)
content = re.sub(r"                    emitOnboardingEvent\(ONBOARDING_EVENTS.FETCH_SUCCESS\);\n", "", content)

with open('frontend/sync.js', 'w', encoding='utf-8') as f:
    f.write(content)
