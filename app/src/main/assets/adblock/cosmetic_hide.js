(function() {
    'use strict';

    const adSelectors = [
        '.ad-container', '.ad-wrapper', '.ad-box', '.ad-banner', '.ad_wrapper',
        '.sponsored-post', '.sponsored-content', '.promoted-link', '.native-ad',
        '[id^="google_ads"]', '[id^="div-gpt-ad"]', 'iframe[src*="ads"]',
        'iframe[src*="doubleclick"]', '.taboola-container', '.outbrain-widget',
        '.popads', '.popcash', '.adsbygoogle', 'aside.ad', 'div.ads-zone'
    ];

    function injectCSS() {
        if (document.getElementById('agy-adblock-styles')) return;
        const style = document.createElement('style');
        style.id = 'agy-adblock-styles';
        style.textContent = adSelectors.join(', ') + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; height: 0 !important; pointer-events: none !important; }';
        (document.head || document.documentElement).appendChild(style);
    }

    function removeAdNodes() {
        const elements = document.querySelectorAll(adSelectors.join(','));
        elements.forEach(el => {
            el.remove();
        });
    }

    injectCSS();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', removeAdNodes);
    } else {
        removeAdNodes();
    }

    // Observer for dynamically inserted ads (SPA / infinity scroll)
    const observer = new MutationObserver((mutations) => {
        removeAdNodes();
    });
    observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
})();
