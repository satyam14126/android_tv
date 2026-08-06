package com.antigravity.tvbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.antigravity.tvbrowser.adblock.AdBlockEngine
import com.antigravity.tvbrowser.bookmark.BookmarkManager
import com.antigravity.tvbrowser.cache.CacheManager
import com.antigravity.tvbrowser.history.HistoryManager
import com.antigravity.tvbrowser.navigation.PointerView
import com.antigravity.tvbrowser.navigation.VirtualCursorController
import com.antigravity.tvbrowser.security.CredentialAutofillBridge
import com.antigravity.tvbrowser.security.EncryptedVaultManager
import com.antigravity.tvbrowser.ui.AddressBarController
import com.antigravity.tvbrowser.ui.CustomWebChromeClient

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var pointerView: PointerView
    private lateinit var cursorController: VirtualCursorController
    private lateinit var adBlockEngine: AdBlockEngine
    private lateinit var cacheManager: CacheManager
    private lateinit var vaultManager: EncryptedVaultManager
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var historyManager: HistoryManager
    private lateinit var webChromeClient: CustomWebChromeClient

    private lateinit var etUrl: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnShieldToggle: LinearLayout
    private lateinit var tvShieldCount: TextView
    private lateinit var ivShieldIcon: ImageView
    private lateinit var btnModeToggle: ImageButton
    private lateinit var btnCache: ImageButton
    private lateinit var btnVault: ImageButton
    private lateinit var btnSettings: ImageButton

    private lateinit var webViewContainer: FrameLayout
    private lateinit var tabStripScroll: HorizontalScrollView
    private lateinit var tabStrip: LinearLayout

    private val tabs = mutableListOf<WebView>()
    private var activeTabIndex = 0
    private var activeWebView: WebView? = null

    companion object {
        private const val DEFAULT_START_URL = "https://www.google.com"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Managers
        adBlockEngine = AdBlockEngine.getInstance(this)
        cacheManager = CacheManager(this)
        vaultManager = EncryptedVaultManager(this)
        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)

        initViews()
        setupWebView()
        setupNavigation()

        // Load Default Start Page
        loadUrl(DEFAULT_START_URL)
    }

    private fun initViews() {
        webView = findViewById(R.id.web_view)
        pointerView = findViewById(R.id.pointer_view)
        etUrl = findViewById(R.id.et_url)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnShieldToggle = findViewById(R.id.btn_shield_toggle)
        tvShieldCount = findViewById(R.id.tv_shield_count)
        ivShieldIcon = findViewById(R.id.iv_shield_icon)
        btnModeToggle = findViewById(R.id.btn_mode_toggle)
        btnCache = findViewById(R.id.btn_cache)
        btnVault = findViewById(R.id.btn_vault)
        btnSettings = findViewById(R.id.btn_settings)
        webViewContainer = findViewById(R.id.webview_container)
        tabStripScroll = findViewById(R.id.tab_strip_scroll)
        tabStrip = findViewById(R.id.tab_strip)

        cursorController = VirtualCursorController(pointerView, webView)

        // Address Bar Listener
        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etUrl.text.toString()
                loadUrl(AddressBarController.formatUrlOrSearch(input))
                activeWebView?.requestFocus()
                true
            } else false
        }

        // Button Click Handlers
        btnBack.setOnClickListener {
            activeWebView?.takeIf { it.canGoBack() }?.goBack()
        }
        btnForward.setOnClickListener {
            activeWebView?.takeIf { it.canGoForward() }?.goForward()
        }
        btnRefresh.setOnClickListener { activeWebView?.reload() }

        btnShieldToggle.setOnClickListener { toggleAdBlockShield() }
        btnModeToggle.setOnClickListener { toggleCursorMode() }
        btnCache.setOnClickListener { showCacheManagerDialog() }
        btnVault.setOnClickListener { showVaultDialog() }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Shared WebChromeClient for Fullscreen Video & Progress across all tabs
        webChromeClient = CustomWebChromeClient(
            findViewById(R.id.fullscreen_video_container),
            findViewById(R.id.webview_container),
            object : CustomWebChromeClient.WebChromeListener {
                override fun onProgressUpdate(newProgress: Int) {}
                override fun onTitleReceived(title: String?) {
                    if (title != null && !etUrl.hasFocus()) {
                        updateShieldCountDisplay()
                    }
                    updateTabChip(activeTabIndex)
                }
                override fun onCreateWindow(resultMsg: Message): Boolean {
                    return openPopupInNewTab(resultMsg)
                }
                override fun onCloseWindow(window: WebView?) {
                    val index = tabs.indexOf(window)
                    if (index >= 0) {
                        closeTab(index)
                    }
                }
            }
        )

        // Base WebView from layout becomes tab #0
        configureWebView(webView)
        tabs.add(webView)
        activeWebView = webView
        webView.visibility = View.VISIBLE
        rebuildTabStrip()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        val settings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        wv.isFocusable = true
        wv.isFocusableInTouchMode = true

        cacheManager.configureCacheSettings(settings)

        // Attach JS Credential Bridge
        val autofillBridge = CredentialAutofillBridge(
            vaultManager,
            object : CredentialAutofillBridge.OnCredentialDetectedListener {
                override fun onFormSubmitDetected(domain: String, user: String, pass: String) {
                    runOnUiThread {
                        showSaveCredentialDialog(domain, user, pass)
                    }
                }
                override fun onRequestAutofill(domain: String) {}
            }
        )
        wv.addJavascriptInterface(autofillBridge, "AGYVault")

        wv.webChromeClient = webChromeClient

        // Custom WebViewClient for Request Interception & Ad-blocking
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request != null && adBlockEngine.shouldBlock(request)) {
                    runOnUiThread { updateShieldCountDisplay() }
                    return adBlockEngine.createEmptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (view == activeWebView) {
                    url?.let { etUrl.setText(it) }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Record visit into persistent browsing history
                url?.let { currentUrl ->
                    val title = view?.title ?: currentUrl
                    historyManager.recordVisit(title, currentUrl)
                }
                // Inject Cosmetic Ad Hiding Script, YouTube ad killer & Autofill listener
                view?.let {
                    adBlockEngine.injectCosmeticHiding(it)
                    url?.let { u -> adBlockEngine.injectSiteSpecificAdBlock(it, u) }
                    it.evaluateJavascript(CredentialAutofillBridge.getAutofillInjectionScript(), null)
                }
                view?.let { v -> updateTabChipForView(v) }
                updateShieldCountDisplay()
            }
        }
    }

    private fun createWebView(): WebView {
        val newView = WebView(this)
        configureWebView(newView)
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        newView.layoutParams = layoutParams
        val insertIndex = webViewContainer.indexOfChild(pointerView)
        webViewContainer.addView(newView, if (insertIndex >= 0) insertIndex else webViewContainer.childCount)
        newView.visibility = View.GONE
        return newView
    }

    private fun newTab(url: String? = null) {
        val newView = createWebView()
        tabs.add(newView)
        activeTabIndex = tabs.size - 1
        activeWebView = newView
        newView.visibility = View.VISIBLE
        activateCurrentTab()
        newView.loadUrl(url ?: DEFAULT_START_URL)
    }

    private fun openPopupInNewTab(resultMsg: Message): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val newView = createWebView()
        tabs.add(newView)
        activeTabIndex = tabs.size - 1
        activeWebView = newView
        newView.visibility = View.VISIBLE
        activateCurrentTab()
        transport.webView = newView
        resultMsg.sendToTarget()
        return true
    }

    private fun switchTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        if (index == activeTabIndex) return
        activeTabIndex = index
        activateCurrentTab()
    }

    private fun closeTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        if (tabs.size <= 1) {
            Toast.makeText(this, R.string.cannot_close_last_tab, Toast.LENGTH_SHORT).show()
            return
        }

        val closedView = tabs.removeAt(index)
        if (index < activeTabIndex) {
            activeTabIndex -= 1
        } else if (index == activeTabIndex) {
            if (activeTabIndex >= tabs.size) activeTabIndex = tabs.size - 1
        }

        webViewContainer.removeView(closedView)
        closedView.removeAllViews()
        closedView.destroy()
        activateCurrentTab()
    }

    private fun activateCurrentTab() {
        if (tabs.isEmpty()) return
        tabs.forEachIndexed { i, v -> v.visibility = if (i == activeTabIndex) View.VISIBLE else View.GONE }
        val current = tabs[activeTabIndex]
        activeWebView = current
        current.requestFocus()
        cursorController.rebind(current)
        current.url?.let { etUrl.setText(it) }
        rebuildTabStrip()
    }

    private fun rebuildTabStrip() {
        tabStrip.removeAllViews()

        // New Tab button
        val addButton = ImageButton(this)
        addButton.setImageResource(android.R.drawable.ic_menu_add)
        addButton.contentDescription = getString(R.string.new_tab)
        addButton.isFocusable = true
        addButton.isClickable = true
        addButton.setBackgroundResource(R.drawable.bg_tv_button)
        addButton.setOnClickListener { newTab() }
        val addLp = LinearLayout.LayoutParams(dp(48), dp(44))
        addLp.rightMargin = dp(10)
        tabStrip.addView(addButton, addLp)

        // Tab chips
        tabs.forEachIndexed { index, view ->
            tabStrip.addView(buildTabChip(index, view))
        }

        // Scroll to the active tab
        tabStripScroll.post {
            tabStripScroll.smoothScrollTo(dp(210) * activeTabIndex, 0)
        }
    }

    private fun buildTabChip(index: Int, view: WebView): View {
        val chip = layoutInflater.inflate(R.layout.item_tab, tabStrip, false)
        val tvTitle = chip.findViewById<TextView>(R.id.tab_title)
        val btnClose = chip.findViewById<ImageButton>(R.id.tab_close)

        tvTitle.text = view.title?.takeIf { it.isNotBlank() } ?: getString(R.string.tab_new_title)
        chip.isSelected = index == activeTabIndex
        chip.isFocusable = true
        chip.isClickable = true
        chip.setOnClickListener { switchTab(index) }
        btnClose.setOnClickListener { closeTab(index) }
        return chip
    }

    private fun updateTabChip(index: Int) {
        if (index < 0 || index >= tabs.size) return
        val child = tabStrip.getChildAt(index + 1) ?: return
        val tvTitle = child.findViewById<TextView>(R.id.tab_title) ?: return
        val title = tabs[index].title?.takeIf { it.isNotBlank() } ?: getString(R.string.tab_new_title)
        if (tvTitle.text.toString() != title) {
            tvTitle.text = title
        }
    }

    private fun updateTabChipForView(view: WebView) {
        val index = tabs.indexOf(view)
        if (index >= 0) updateTabChip(index)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun setupNavigation() {
        // Default to TV Virtual Pointer Mode
        cursorController.isCursorModeEnabled = true
    }

    private fun loadUrl(url: String) {
        activeWebView?.loadUrl(url)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (webChromeClient.isFullscreen()) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                webChromeClient.onHideCustomView()
                return true
            }
        }

        // If top bar, tab strip, or EditText has focus, pass key to standard focus engine
        if (etUrl.hasFocus() || btnBack.hasFocus() || btnForward.hasFocus() ||
            btnRefresh.hasFocus() || btnModeToggle.hasFocus() || btnCache.hasFocus() ||
            btnVault.hasFocus() || btnSettings.hasFocus() || tabStrip.hasFocus()
        ) {
            return super.dispatchKeyEvent(event)
        }

        // Otherwise handle D-pad key events via VirtualCursorController
        if (cursorController.handleKeyEvent(event)) {
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        val current = activeWebView
        if (current != null && current.canGoBack()) {
            current.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tabs.forEach { tab ->
            webViewContainer.removeView(tab)
            tab.removeAllViews()
            tab.destroy()
        }
        tabs.clear()
    }

    private fun updateShieldCountDisplay() {
        val count = adBlockEngine.blockedCount.get()
        tvShieldCount.text = "$count Blocked"
    }

    private fun toggleAdBlockShield() {
        adBlockEngine.isEnabled = !adBlockEngine.isEnabled
        val status = if (adBlockEngine.isEnabled) "Shield Activated" else "Shield Disabled"
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        updateShieldCountDisplay()
        activeWebView?.reload()
    }

    private fun toggleCursorMode() {
        cursorController.isCursorModeEnabled = !cursorController.isCursorModeEnabled
        val modeName = if (cursorController.isCursorModeEnabled) "Pointer Mode" else "D-Pad Focus Mode"
        Toast.makeText(this, "Switched to $modeName", Toast.LENGTH_SHORT).show()
    }

    private fun showCacheManagerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cache_manager, null)
        val tvCacheInfo = dialogView.findViewById<TextView>(R.id.tv_cache_info)
        val btnClearCache = dialogView.findViewById<Button>(R.id.btn_clear_cache_action)
        val btnClearCookies = dialogView.findViewById<Button>(R.id.btn_clear_cookies_action)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_cache)

        val sizeMb = cacheManager.calculateCacheSizeMB()
        tvCacheInfo.text = "Estimated Cache Size: " + String.format("%.2f MB", sizeMb)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClearCache.setOnClickListener {
            tabs.forEach { cacheManager.clearWebCache(it) }
            cacheManager.clearWebStorage()
            Toast.makeText(this, "Cache & Storage Cleared!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClearCookies.setOnClickListener {
            cacheManager.clearCookies()
            Toast.makeText(this, "Cookies Removed!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showVaultDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vault, null)
        val lvItems = dialogView.findViewById<ListView>(R.id.lv_vault_items)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_vault)

        val creds = vaultManager.getAllCredentials()
        val itemStrings = creds.map { "${it.domain} (${it.username})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemStrings)
        lvItems.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showSaveCredentialDialog(domain: String, user: String, pass: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_credential_save, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        tvMessage.text = "Would you like to securely encrypt and save credentials for $domain on this TV?"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            vaultManager.saveCredential(domain, user, pass)
            Toast.makeText(this, "Login credentials saved in Vault!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val btnHistory = dialogView.findViewById<Button>(R.id.btn_settings_history)
        val btnSaveBookmark = dialogView.findViewById<Button>(R.id.btn_settings_save_bookmark)
        val btnBookmarks = dialogView.findViewById<Button>(R.id.btn_settings_bookmarks)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_settings)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnHistory.setOnClickListener {
            dialog.dismiss()
            showHistoryDialog()
        }

        btnSaveBookmark.setOnClickListener {
            dialog.dismiss()
            saveCurrentBookmark()
        }

        btnBookmarks.setOnClickListener {
            dialog.dismiss()
            showBookmarksDialog()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveCurrentBookmark() {
        val currentWebView = activeWebView
        val currentUrl = currentWebView?.url ?: etUrl.text.toString()
        if (currentUrl.isBlank() || currentUrl == "about:blank") {
            Toast.makeText(this, R.string.bookmark_save_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val title = currentWebView?.title ?: currentUrl
        val saved = bookmarkManager.saveBookmark(title, currentUrl)
        val message = if (saved) R.string.bookmark_saved else R.string.already_bookmarked
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showHistoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_history, null)
        val lvItems = dialogView.findViewById<ListView>(R.id.lv_history_items)
        val btnClear = dialogView.findViewById<Button>(R.id.btn_clear_history)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_history)

        val history = historyManager.getAllHistory()
        if (history.isEmpty()) {
            Toast.makeText(this, R.string.empty_history, Toast.LENGTH_SHORT).show()
            return
        }

        val itemStrings = history.map { entry ->
            val time = java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT
            ).format(java.util.Date(entry.timestamp))
            "${entry.title} — ${entry.url} ($time)"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemStrings)
        lvItems.adapter = adapter
        lvItems.setOnItemClickListener { _, _, position, _ ->
            val entry = history[position]
            loadUrl(entry.url)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClear.setOnClickListener {
            historyManager.clearHistory()
            Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showBookmarksDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bookmarks, null)
        val lvItems = dialogView.findViewById<ListView>(R.id.lv_bookmark_items)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_bookmarks)

        val bookmarks = bookmarkManager.getAllBookmarks()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, R.string.empty_bookmarks, Toast.LENGTH_SHORT).show()
            return
        }

        val itemStrings = bookmarks.map { "${it.title} — ${it.url}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemStrings)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        lvItems.adapter = adapter
        lvItems.setOnItemClickListener { _, _, position, _ ->
            val bookmark = bookmarks[position]
            loadUrl(bookmark.url)
        }
        lvItems.setOnItemLongClickListener { _, _, position, _ ->
            val bookmark = bookmarks[position]
            bookmarkManager.deleteBookmark(bookmark.url)
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            showBookmarksDialog()
            true
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
