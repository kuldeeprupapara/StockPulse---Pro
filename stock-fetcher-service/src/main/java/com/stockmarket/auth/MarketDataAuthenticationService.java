package com.stockmarket.auth;

import com.stockmarket.config.MarketDataProviderConfig;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataAuthenticationService {

    private final MarketDataProviderConfig config;

    private volatile AuthenticationContext authContext;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void init() throws IOException {
        log.info("Started fetching cookies & crumb");
        log.info("Market data authentication service initialized");
        log.info("Auto-refresh interval: {} minutes", config.getCrumbRefreshIntervalMinutes());
        this.initializeAuthContext();
    }

    public AuthenticationContext authContext() {
        return authContext;
    }
    /**
     * Get current authentication context (cookies + crumb).
     * Automatically refreshes if expired.
     */
    private void initializeAuthContext() throws IOException {
        // Fast path - read lock
        lock.readLock().lock();
        try {
            if (authContext != null && authContext.isValid(config.getCrumbRefreshIntervalMillis())) {
                log.debug("Using cached authentication context");
                return;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path - write lock and refresh
        refreshAuthContext();
    }

    /**
     * Force refresh authentication context.
     */
    public void refreshAuthContext() throws IOException {
        lock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            if (authContext != null && authContext.isValid(config.getCrumbRefreshIntervalMillis())) {
                log.debug("Another thread already refreshed, using existing context");
                return;
            }

            log.info("Refreshing authentication context...");

            // Step 1: Fetch cookies
            Map<String, String> cookies = fetchCookies();

            // Step 2: Fetch crumb using cookies
            String crumb = fetchCrumb(cookies);

            // Create new context
            authContext = new AuthenticationContext(
                    cookies,
                    crumb,
                    System.currentTimeMillis()
            );

            log.info("Authentication context successfully refreshed : {}", authContext);

            log.info("Authentication context refreshed successfully. Has crumb: {}", crumb != null);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Fetch cookies from configured URLs.
     */
    private Map<String, String> fetchCookies() throws IOException {
        Map<String, String> cookies = null;
        IOException lastException = null;

        for (String cookieUrl : config.getCookieUrls()) {
            try {
                log.debug("Attempting to fetch cookies from: {}", cookieUrl);

                Connection.Response response = Jsoup.connect(cookieUrl)
                        .userAgent(config.getUserAgent())
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", config.getAcceptLanguage())
                        .followRedirects(true)
                        .timeout(config.getTimeoutMillis())
                        .execute();

                cookies = response.cookies();

                if (!cookies.isEmpty()) {
                    log.info("Successfully obtained {} cookies from {}", cookies.size(), cookieUrl);
                    log.debug("Cookie names: {}", cookies.keySet());
                    return cookies;
                }

            } catch (IOException e) {
                log.warn("Failed to get cookies from {}: {}", cookieUrl, e.getMessage());
                lastException = e;
            }
        }

        log.error("Failed to obtain cookies from all configured endpoints");
        throw lastException != null ? lastException :
                new IOException("Failed to obtain cookies from market data provider");
    }

    /**
     * Fetch crumb using the provided cookies.
     */
    private String fetchCrumb(Map<String, String> cookies) {
        try {
            String crumbUrl = config.getBaseUrl() + config.getCrumbEndpoint();
            log.debug("Fetching crumb from: {}", crumbUrl);

            Connection.Response response = Jsoup.connect(crumbUrl)
                    .ignoreContentType(true)
                    .userAgent(config.getUserAgent())
                    .cookies(cookies)
                    .header("Accept", "*/*")
                    .header("Accept-Language", config.getAcceptLanguage())
                    .header("Referer", config.getReferer())
                    .timeout(config.getTimeoutMillis())
                    .execute();

            String crumb = response.body().trim();

            if (!crumb.isEmpty() &&
                    !crumb.contains("error") &&
                    !crumb.contains("<!DOCTYPE")) {
                log.info("Successfully obtained crumb: {}...",
                        crumb.substring(0, Math.min(10, crumb.length())));
                return crumb;
            } else {
                log.warn("Crumb response seems invalid: {}", crumb);
                return null;
            }

        } catch (IOException e) {
            log.warn("Failed to obtain crumb (will continue without it): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Invalidate current authentication context (forces refresh on next call).
     */
    public void invalidate() {
        lock.writeLock().lock();
        try {
            log.info("Invalidating authentication context");
            authContext = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if current context is valid.
     */
    public boolean isValid() {
        lock.readLock().lock();
        try {
            return authContext != null && authContext.isValid(config.getCrumbRefreshIntervalMillis());
        } finally {
            lock.readLock().unlock();
        }
    }
}
