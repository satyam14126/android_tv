package com.antigravity.tvbrowser.security

import android.webkit.JavascriptInterface

class CredentialAutofillBridge(
    private val vaultManager: EncryptedVaultManager,
    private val listener: OnCredentialDetectedListener
) {

    interface OnCredentialDetectedListener {
        fun onFormSubmitDetected(domain: String, user: String, pass: String)
        fun onRequestAutofill(domain: String)
    }

    @JavascriptInterface
    fun onFormSubmitted(domain: String, user: String, pass: String) {
        if (user.isNotEmpty() && pass.isNotEmpty()) {
            listener.onFormSubmitDetected(domain, user, pass)
        }
    }

    @JavascriptInterface
    fun checkAutofillAvailable(domain: String): String {
        val cred = vaultManager.getCredential(domain)
        return if (cred != null) {
            "{\"user\":\"" + cred.username + "\", \"pass\":\"" + cred.secretHash + "\"}"
        } else {
            ""
        }
    }

    companion object {
        fun getAutofillInjectionScript(): String {
            return """
                (function() {
                    function attachListeners() {
                        var forms = document.querySelectorAll('form');
                        forms.forEach(function(form) {
                            form.addEventListener('submit', function() {
                                var passInput = form.querySelector('input[type="password"]');
                                var userInput = form.querySelector('input[type="text"], input[type="email"]');
                                if (passInput && passInput.value) {
                                    var u = userInput ? userInput.value : '';
                                    var p = passInput.value;
                                    var domain = window.location.hostname;
                                    if (window.AGYVault) {
                                        window.AGYVault.onFormSubmitted(domain, u, p);
                                    }
                                }
                            });
                        });
                    }

                    function tryAutofill() {
                        if (!window.AGYVault) return;
                        var domain = window.location.hostname;
                        var res = window.AGYVault.checkAutofillAvailable(domain);
                        if (res) {
                            try {
                                var data = JSON.parse(res);
                                var passInput = document.querySelector('input[type="password"]');
                                var userInput = document.querySelector('input[type="text"], input[type="email"]');
                                if (userInput && !userInput.value) userInput.value = data.user;
                                if (passInput && !passInput.value) passInput.value = data.pass;
                            } catch(e){}
                        }
                    }

                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', function() {
                            attachListeners();
                            tryAutofill();
                        });
                    } else {
                        attachListeners();
                        tryAutofill();
                    }
                })();
            """.trimIndent()
        }
    }
}
