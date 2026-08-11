package com.clhs.score.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.clhs.score.data.AuthenticatedSession
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val LOGIN_URL = "https://shcloud2.k12ea.gov.tw/CLHSTYC/Auth/Auth/CloudLogin"
private const val SCHOOL_HOME_URL = "https://shcloud2.k12ea.gov.tw/CLHSTYC/ICampus/Home/Index2"
private const val SCHOOL_DOMAIN = "shcloud2.k12ea.gov.tw"
private const val LOGIN_HOOK_LOG_PREFIX = "[ScoreLoginHook]"
private const val LOGIN_HOOK_SUCCESS_PREFIX = "$LOGIN_HOOK_LOG_PREFIX LoginSuccess "
private const val WEB_VIEW_LOGIN_TAG = "WebViewLogin"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebViewLoginScreen(
    isProcessingLogin: Boolean,
    errorMessage: String?,
    onLoginSuccess: (studentNo: String, cookieString: String) -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    var isPageLoading by remember { mutableStateOf(true) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val utilityMotion = remember { MotionScheme.standard() }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.clearSchoolWebData(clearCookies = true)
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        WebViewContent(
            onWebViewCreated = { webViewRef = it },
            onPageStarted = { isPageLoading = true },
            onPageFinished = { isPageLoading = false },
            onProgressChanged = { pageProgress = it / 100f },
            onLoginSuccess = onLoginSuccess,
        )

        AnimatedVisibility(
            visible = isPageLoading || isProcessingLogin,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            LinearProgressIndicator(
                progress = { if (isProcessingLogin) 1f else pageProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WebViewNavigationControls(webView = webViewRef, onBack = onBack)

        AnimatedVisibility(
            visible = isProcessingLogin,
            enter = fadeIn(utilityMotion.defaultEffectsSpec()),
            exit = fadeOut(utilityMotion.defaultEffectsSpec()),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.shapes.large,
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "正在建立連線…",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.medium,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
fun SchoolWebsiteScreen(
    session: AuthenticatedSession,
    onBack: () -> Unit,
) {
    var isPageLoading by remember { mutableStateOf(true) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.clearSchoolWebData(clearCookies = true)
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        AuthenticatedSchoolWebView(
            session = session,
            onWebViewCreated = { webViewRef = it },
            onPageStarted = { isPageLoading = true },
            onPageFinished = { isPageLoading = false },
            onProgressChanged = { pageProgress = it / 100f },
        )

        AnimatedVisibility(
            visible = isPageLoading,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            LinearProgressIndicator(
                progress = { pageProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WebViewNavigationControls(webView = webViewRef, onBack = onBack)
    }
}

@Composable
private fun WebViewNavigationControls(
    webView: WebView?,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            OutlinedRoundedSymbol(
                icon = "arrow_back",
                contentDescription = "返回",
            )
        }

        IconButton(
            onClick = { webView?.reload() },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            OutlinedRoundedSymbol(
                icon = "refresh",
                contentDescription = "重新載入",
            )
        }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AuthenticatedSchoolWebView(
    session: AuthenticatedSession,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onProgressChanged: (Int) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                configureForSchoolSite()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val url = request?.url?.toString() ?: return true
                        return !isTrustedSchoolUrl(url)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished()
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }
                onWebViewCreated(this)
                loadAuthenticatedSchoolSite(session)
            }
        },
    )
}

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContent(
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLoginSuccess: (studentNo: String, cookieString: String) -> Unit,
) {
    var loginHandled by remember { mutableStateOf(false) }
    var isTrustedLoginPage by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            android.webkit.WebView(context).apply {
                configureForSchoolSite()

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val url = request?.url?.toString() ?: return true
                        return !isTrustedSchoolUrl(url)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        isTrustedLoginPage = false
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished()
                        isTrustedLoginPage = isTrustedSchoolLoginUrl(url)
                        if (isTrustedLoginPage) {
                            loginHandled = false
                            view?.evaluateJavascript(LOGIN_HOOK_JS, null)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message().orEmpty()
                        if (message.startsWith(LOGIN_HOOK_SUCCESS_PREFIX)) {
                            if (loginHandled || !isTrustedLoginPage) return true
                            loginHandled = true
                            val studentNo = URLDecoder.decode(
                                message.removePrefix(LOGIN_HOOK_SUCCESS_PREFIX),
                                StandardCharsets.UTF_8.name(),
                            )
                            val cookieString = CookieManager.getInstance()
                                .getCookie("https://$SCHOOL_DOMAIN") ?: ""
                            post { onLoginSuccess(studentNo, cookieString) }
                            return true
                        }
                        if (message.startsWith(LOGIN_HOOK_LOG_PREFIX)) {
                            Log.w(WEB_VIEW_LOGIN_TAG, message)
                            return true
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                onWebViewCreated(this)
                loadUrl(LOGIN_URL)
            }
        },
    )
}

private fun isTrustedSchoolUrl(url: String?): Boolean {
    val uri = runCatching { url?.toUri() }.getOrNull() ?: return false
    return uri.scheme == "https" && uri.host.equals(SCHOOL_DOMAIN, ignoreCase = true)
}

private fun isTrustedSchoolLoginUrl(url: String?): Boolean {
    val uri = runCatching { url?.toUri() }.getOrNull() ?: return false
    return isTrustedSchoolUrl(url) &&
        uri.encodedPath.orEmpty().contains("/CLHSTYC/Auth/Auth/CloudLogin")
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureForSchoolSite() {
    layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
    )
    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        textZoom = 100
        cacheMode = WebSettings.LOAD_NO_CACHE
        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        loadWithOverviewMode = true
        useWideViewPort = true
        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        saveFormData = false
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@configureForSchoolSite, false)
    }
}

private fun WebView.loadAuthenticatedSchoolSite(session: AuthenticatedSession) {
    val cookieManager = CookieManager.getInstance()
    session.cookies.forEach { (name, value) ->
        cookieManager.setCookie(SCHOOL_HOME_URL, "$name=$value; Path=/; Secure")
    }
    cookieManager.flush()
    loadUrl(SCHOOL_HOME_URL)
}

private fun WebView.clearSchoolWebData(clearCookies: Boolean) {
    stopLoading()
    clearHistory()
    clearFormData()
    clearCache(true)
    WebStorage.getInstance().deleteAllData()
    if (clearCookies) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}

private val LOGIN_HOOK_JS = """
(function() {
    if (window.__loginHooked) return;
    window.__loginHooked = true;

    var origOpen = XMLHttpRequest.prototype.open;
    var origSend = XMLHttpRequest.prototype.send;

    XMLHttpRequest.prototype.open = function(method, url) {
        this.__url = url;
        this.__method = method;
        return origOpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function(body) {
        var self = this;
        if (self.__url && self.__url.indexOf('DoCloudLoginCheck') >= 0) {
            var loginId = '';
            try {
                var loginField = document.querySelector('input[name="LoginId"]');
                if (loginField) loginId = loginField.value || '';
            } catch(e) {
                console.warn('$LOGIN_HOOK_LOG_PREFIX LoginId field lookup failed: ' + (e && e.message ? e.message : e));
            }

            if (!loginId && body) {
                try {
                    var params = new URLSearchParams(body);
                    loginId = params.get('LoginId') || '';
                } catch(e) {
                    console.warn('$LOGIN_HOOK_LOG_PREFIX LoginId body parse failed: ' + (e && e.message ? e.message : e));
                }
            }

            self.addEventListener('load', function() {
                try {
                    var resp = JSON.parse(self.responseText);
                    if (resp && resp.Result && resp.Result.IsLoginSuccess === true) {
                        console.info('$LOGIN_HOOK_SUCCESS_PREFIX' + encodeURIComponent(loginId));
                    }
                } catch(e) {
                    console.warn('$LOGIN_HOOK_LOG_PREFIX Login response handling failed: ' + (e && e.message ? e.message : e));
                }
            });
        }
        return origSend.apply(this, arguments);
    };
})();
""".trimIndent()
