package com.antigravity.tvbrowser.ui

import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView

class CustomWebChromeClient(
    private val videoContainer: ViewGroup,
    private val webViewContainer: ViewGroup,
    private val listener: WebChromeListener
) : WebChromeClient() {

    interface WebChromeListener {
        fun onProgressUpdate(newProgress: Int)
        fun onTitleReceived(title: String?)
        fun onCreateWindow(resultMsg: Message): Boolean
        fun onCloseWindow(window: WebView?)
    }

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        listener.onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        listener.onTitleReceived(title)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        return if (resultMsg != null) {
            listener.onCreateWindow(resultMsg)
        } else {
            false
        }
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        listener.onCloseWindow(window)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            onHideCustomView()
            return
        }
        customView = view
        customViewCallback = callback

        videoContainer.addView(view)
        videoContainer.visibility = View.VISIBLE
        webViewContainer.visibility = View.GONE
    }

    override fun onHideCustomView() {
        if (customView == null) return

        videoContainer.removeView(customView)
        videoContainer.visibility = View.GONE
        webViewContainer.visibility = View.VISIBLE

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    fun isFullscreen(): Boolean = customView != null
}
