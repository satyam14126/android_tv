(function () {
    'use strict';

    if (window.agYtAdBlockInjected) return;
    window.agYtAdBlockInjected = true;

    var AD_SELECTORS = [
        '.ytp-ad-module',
        '.ytp-ad-player-overlay',
        '.ytp-ad-overlay-container',
        '.ytp-ad-image-overlay',
        '.ytp-ad-text-overlay',
        '.ytp-ad-skip-button-container',
        '.ytp-ad-action-interstitial-slot',
        '.ytp-ad-info-card',
        '.ytp-ad-progress-list',
        '.ytp-ad-badge',
        '.ytp-ad-text',
        '.ytp-ad-simple-ad-badge',
        '.ytp-ad-skip-button-slot',
        '#player-ads',
        '#masthead-ad',
        '.ad-container',
        'ytd-display-ad-renderer',
        'ytd-in-feed-ad-layout-renderer',
        'ytd-promoted-sparkles-web-renderer',
        'ytd-ad-slot-renderer',
        'ytd-banner-promo-renderer',
        'ytd-statement-banner-renderer',
        'ytd-banner-ad-layout-renderer',
        'ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"]',
        'ytd-compact-promoted-video-renderer',
        'ytd-promoted-sparkles-text-search-renderer',
        'ytd-search-pyv-renderer',
        'ytd-player-legacy-desktop-watch-ads-renderer',
        'ytd-merch-shelf-renderer[is-marquee]',
        '.ytd-merch-shelf-renderer[is-marquee]',
        '.grippy-host',
        'ytd-mealbar-promo-renderer',
        'ytd-popup-container ytd-enforcement-message-view-model',
        '#movie-offer-module',
        '#offers-module'
    ];

    function hideAds() {
        var nodes = document.querySelectorAll(AD_SELECTORS.join(','));
        for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            if (el && el.parentNode) el.remove();
        }
    }

    function clickSkipButton() {
        var selectors = [
            '.ytp-ad-skip-button',
            '.ytp-skip-ad-button',
            '.ytp-ad-skip-button-modern',
            'button.ytp-ad-skip-button',
            'button.ytp-skip-ad-button',
            'button.ytp-ad-skip-button-modern',
            '.ytp-ad-skip-button-slot button',
            '.ytp-ad-skip-button-slot div'
        ];
        for (var i = 0; i < selectors.length; i++) {
            var btn = document.querySelector(selectors[i]);
            if (btn) {
                try { btn.click(); } catch (e) {}
                return true;
            }
        }
        return false;
    }

    function isAdShowing() {
        var video = document.querySelector('video');
        if (!video) return false;
        // YouTube marks ads by adding classes on the player / ad server params
        var player = document.querySelector('.html5-video-player');
        if (player) {
            var cls = player.className || '';
            if (cls.indexOf('ad-showing') !== -1 || cls.indexOf('ad-interrupting') !== -1) return true;
        }
        // Overlay ads present
        if (document.querySelector('.ytp-ad-player-overlay, .ytp-ad-module, .ytp-ad-image-overlay')) return true;
        return false;
    }

    function skipPlayingAd() {
        if (clickSkipButton()) return;
        if (!isAdShowing()) return;

        var video = document.querySelector('video');
        if (!video) return;

        try {
            // Fast-forward through the ad by seeking to its end
            if (video.duration && isFinite(video.duration) && video.duration > 0) {
                video.currentTime = video.duration;
            }
        } catch (e) {}
    }

    function blockAds() {
        hideAds();
        skipPlayingAd();
    }

    // Initial run + DOMContentLoaded for late-rendered ads
    blockAds();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', blockAds);
    }

    // Observe the whole document for dynamically injected ads
    var observer = new MutationObserver(function () {
        blockAds();
    });
    if (document.documentElement) {
        observer.observe(document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'src']
        });
    }

    // Periodic sweep as a safety net for SPA navigation / live streams
    window.setInterval(blockAds, 2000);
})();
