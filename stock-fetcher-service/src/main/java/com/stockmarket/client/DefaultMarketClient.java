package com.stockmarket.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.auth.MarketDataAuthenticationService;
import com.stockmarket.config.MarketDataProviderConfig;
import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartPoint;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.SearchResult;
import com.stockmarket.model.StockQuoteEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMarketClient implements ExternalMarketClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final MarketDataProviderConfig config;
    private final MarketDataAuthenticationService authService;
    private final HttpClientHelper httpClient;

    @Override
    public StockQuoteEvent fetchQuote(String symbol) {
        return this.fetchQuotes(List.of(symbol)).get(0);
    }

    @Override
    public List<StockQuoteEvent> fetchQuotes(List<String> symbols) {
        try {
            String joinedSymbols = String.join(",", symbols);

            // Build URL with authentication
            Map<String, String> params = new HashMap<>();
            params.put("symbols", joinedSymbols);

            String url = httpClient.buildUrlWithAuth(config.getQuoteEndpoint(), params);

            log.debug("Fetching quotes for symbols: {}", joinedSymbols);

            // Execute with automatic retry on unauthorized
            String response = httpClient.executeGetWithRetry(url, config.getMaxRetries());

            // Parse response
            JsonNode root = MAPPER.readTree(response);

            log.debug("Quote response payload: {}", root);
            // Check for error
            JsonNode errorNode = root.path("finance").path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String errorCode = errorNode.path("code").asText();
                String errorDesc = errorNode.path("description").asText();
                log.error("Market data API error - Code: {}, Description: {}", errorCode, errorDesc);
                throw new RuntimeException("Market data API error: " + errorCode + " - " + errorDesc);
            }

            JsonNode results = root.path("quoteResponse").path("result");

            if (results.isMissingNode() || !results.isArray()) {
                log.warn("No results found in response for symbols: {}", joinedSymbols);
                return new ArrayList<>();
            }

            List<StockQuoteEvent> quotes = new ArrayList<>();
            for (JsonNode node : results) {
                quotes.add(map(node));
            }

            log.info("Successfully fetched {} quotes", quotes.size());
            return quotes;

        } catch (IOException ex) {
            log.error("Failed to fetch quotes", ex);
            throw new RuntimeException("Failed to fetch market quotes", ex);
        }
    }

    @Override
    public ChartResponse fetchChart(String symbol, String interval, String range) {
        try {
//            String url =
//                    "https://query2.finance.yahoo.com/v8/finance/chart/" + symbol +
//                            "?interval=" + interval +
//                            "&range=" + range;
            final Map<String, String> params = new HashMap<>();
            params.put("interval", interval);
            params.put("range", range);
            String url = httpClient.buildUrlWithAuth(config.getChartEndpoint(), symbol, params);
            log.info("Chart URL: {}", url);

            String response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36"
                    )
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://finance.yahoo.com/")
                    .timeout(15_000)
                    .execute()
                    .body();

            JsonNode root = MAPPER.readTree(response);
            JsonNode result =
                    root.path("chart").path("result").get(0);

            if (result == null || result.isMissingNode()) {
                final ChartResponse chartResponse = new ChartResponse();
                chartResponse.setSymbol(symbol);
                chartResponse.setInterval(interval);
                chartResponse.setRange(range);
                chartResponse.setData(Collections.emptyList());
                return chartResponse;
            }

            log.debug("Chart response result node: {}", result);

            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);

            JsonNode opens = quote.path("open");
            JsonNode highs = quote.path("high");
            JsonNode lows = quote.path("low");
            JsonNode closes = quote.path("close");
            JsonNode volumes = quote.path("volume");

            List<ChartPoint> points = new ArrayList<>();

            for (int i = 0; i < timestamps.size(); i++) {
                if (opens.get(i).isNull()) {
                    continue;
                }

                final ChartPoint point = new ChartPoint();
                point.setTimestamp(timestamps.get(i).asLong() * 1000);
                point.setOpen(opens.get(i).asDouble());
                point.setHigh(highs.get(i).asDouble());
                point.setLow(lows.get(i).asDouble());
                point.setClose(closes.get(i).asDouble());
                point.setVolume(volumes.get(i).asLong());

                points.add(point);
            }

            final ChartResponse chartResponse = new ChartResponse();
            chartResponse.setSymbol(symbol);
            chartResponse.setInterval(interval);
            chartResponse.setData(points);
            chartResponse.setRange(range);
            return chartResponse;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch chart data", ex);
        }
    }

    @Override
    public List<SearchResult> search(String query) {

        try {
            final Map<String, String> params = new HashMap<>();
            params.put("q", query);

            String url = httpClient.buildUrlWithAuth(config.getSearchEndPoint(), params);


            String response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36"
                    )
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://finance.yahoo.com/")
                    .timeout(15_0000)
                    .execute()
                    .body();

            JsonNode root = MAPPER.readTree(response);
            JsonNode quotes = root.path("quotes");

            List<SearchResult> results = new ArrayList<>();

            for (JsonNode node : quotes) {
                // Filter out non-tradable junk
                if (!node.hasNonNull("symbol") || !node.hasNonNull("shortname")) {
                    continue;
                }

                final SearchResult result = new SearchResult();
                result.setSymbol(node.path("symbol").asText());
                result.setName(node.path("shortname").asText());
                result.setExchange(node.path("exchange").asText());
                result.setType(node.path("quoteType").asText());

                results.add(result);
            }
            return results;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to search market symbols", ex);
        }
    }

    @Override
    public CompanySummary fetchCompanySummary(String symbol) {
        int attempts   = 0;
        int maxRetries = config.getMaxRetries();

        while (attempts < maxRetries) {
            attempts++;
            try {
                final Map<String, String> params = new HashMap<>();
                params.put("modules",
                        "price,summaryDetail,defaultKeyStatistics,assetProfile,calendarEvents,financialData");

                String url = httpClient.buildUrlWithAuth(
                        config.getCompanySummaryEndpoint(), symbol, params);

                log.info("Fetching company summary for {} (attempt {})", symbol, attempts);
                log.debug("URL: {}", url);

                String   response = httpClient.executeGet(url);
                JsonNode root     = MAPPER.readTree(response);
                log.debug("Company summary payload for {}: {}", symbol, root);

                // ── Error check ───────────────────────────────────────────────────
                JsonNode errorNode = root.path("finance").path("error");
                if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                    String code = errorNode.path("code").asText();
                    String desc = errorNode.path("description").asText();
                    log.warn("API error - Code: {}, Description: {}", code, desc);

                    if ("Unauthorized".equals(code) && attempts < maxRetries) {
                        log.warn("Invalid crumb/auth, refreshing and retrying...");
                        authService.invalidate();
                        authService.refreshAuthContext();
                        continue;
                    }
                    throw new RuntimeException("API error: " + code + " - " + desc);
                }

                JsonNode result = root.path("quoteSummary").path("result").get(0);
                if (result == null || result.isMissingNode()) {
                    throw new RuntimeException("No summary data available for " + symbol);
                }

                // ── Module nodes ──────────────────────────────────────────────────
                JsonNode price    = result.path("price");
                JsonNode detail   = result.path("summaryDetail");
                JsonNode stats    = result.path("defaultKeyStatistics");
                JsonNode profile  = result.path("assetProfile");
                JsonNode calendar = result.path("calendarEvents");
                JsonNode finData  = result.path("financialData");

                // ── Address ───────────────────────────────────────────────────────
                String address1 = profile.path("address1").asText("");
                String address2 = profile.path("address2").asText("");
                String city     = profile.path("city").asText("");
                String zipcode  = profile.path("zip").asText("");
                String country  = profile.path("country").asText("");
                String phone    = profile.path("phone").asText("");
                String website  = profile.path("website").asText("");

                String address = Stream.of(
                                address1,
                                address2,
                                city + (zipcode.isBlank() ? "" : " – " + zipcode),
                                country, phone, website
                        ).filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(System.lineSeparator()));

                // ── Earnings date ─────────────────────────────────────────────────
                JsonNode earningsDateNode = calendar.path("earnings").path("earningsDate");
                String earningsDate = "";
                if (earningsDateNode.isArray() && earningsDateNode.size() > 0) {
                    earningsDate = earningsDateNode.get(0).path("fmt").asText("");
                }

                // ── P/E: prefer trailing, fall back to forward ────────────────────
                double peRatio = stats.path("trailingPE").path("raw").asDouble(
                        stats.path("forwardPE").path("raw").asDouble(0)
                );

                // ── Derived values ────────────────────────────────────────────────
                double previousClose  = price.path("regularMarketPreviousClose").path("raw").asDouble(0);
                double marketCapRaw   = detail.path("marketCap").path("raw").asDouble(0);

                // ── Build DTO ─────────────────────────────────────────────────────
                final CompanySummary cs = new CompanySummary();

                // Identity
                cs.setSymbol(symbol);
                cs.setCompanyName(price.path("longName").asText(""));
                cs.setSector(profile.path("sector").asText(""));
                cs.setIndustry(profile.path("industry").asText(""));
                cs.setExchange(price.path("exchangeName").asText(""));
                cs.setMarketCapCategory(resolveMarketCapCategory(marketCapRaw));
                cs.setBusinessSummary(profile.path("longBusinessSummary").asText(""));
                cs.setAddress(address);
                cs.setWebsite(website);
                cs.setFullTimeEmployees(profile.path("fullTimeEmployees").asInt(0));
                cs.setEarningsDate(earningsDate);

                // Live price
                cs.setRegularMarketPrice(price.path("regularMarketPrice").path("raw").asDouble(0));
                cs.setRegularMarketChange(price.path("regularMarketChange").path("raw").asDouble(0));
                cs.setRegularMarketChangePercent(
                        price.path("regularMarketChangePercent").path("raw").asDouble(0) * 100);
                cs.setRegularMarketDayOpen(price.path("regularMarketOpen").path("raw").asDouble(0));
                cs.setRegularMarketDayHigh(price.path("regularMarketDayHigh").path("raw").asDouble(0));
                cs.setRegularMarketDayLow(price.path("regularMarketDayLow").path("raw").asDouble(0));
                cs.setRegularMarketVolume(price.path("regularMarketVolume").path("raw").asLong(0));
                cs.setRegularMarketPreviousClose(previousClose);
                cs.setMarketState(price.path("marketState").asText("UNKNOWN"));

                // 52-Week
                cs.setWeek52High(detail.path("fiftyTwoWeekHigh").path("raw").asDouble(0));
                cs.setWeek52Low(detail.path("fiftyTwoWeekLow").path("raw").asDouble(0));
                cs.setWeek52Change(stats.path("52WeekChange").path("raw").asDouble(0) * 100);

                // Market Cap
                cs.setMarketCap(marketCapRaw);
                cs.setMarketCapFormatted(formatToLakhCrore(marketCapRaw));

                // Valuation
                cs.setPeRatio(peRatio);
                cs.setForwardPE(stats.path("forwardPE").path("raw").asDouble(0));
                cs.setPriceToBook(stats.path("priceToBook").path("raw").asDouble(0));
                cs.setPriceToSales(detail.path("priceToSalesTrailing12Months").path("raw").asDouble(0));

                // Moving Averages
                cs.setFiftyDayAverage(detail.path("fiftyDayAverage").path("raw").asDouble(0));
                cs.setTwoHundredDayAverage(detail.path("twoHundredDayAverage").path("raw").asDouble(0));

                // Dividends (yield stored as %, e.g. 0.43)
                cs.setDividendRate(detail.path("dividendRate").path("raw").asDouble(0));
                cs.setDividendYield(detail.path("dividendYield").path("raw").asDouble(0) * 100);
                cs.setExDividendDate(detail.path("exDividendDate").path("fmt").asText(""));
                cs.setPayoutRatio(detail.path("payoutRatio").path("raw").asDouble(0) * 100);

                // EPS
                cs.setEpsTrailingTwelveMonths(stats.path("trailingEps").path("raw").asDouble(0));
                cs.setEpsForward(stats.path("forwardEps").path("raw").asDouble(0));

                // Volume
                cs.setAverageVolume3Month(detail.path("averageVolume").path("raw").asLong(0));
                cs.setAverageVolume10Day(detail.path("averageVolume10days").path("raw").asLong(0));

                // Earnings & Margins
                cs.setProfitMargin(stats.path("profitMargins").path("raw").asDouble(0) * 100);
                cs.setEarningsGrowthQoQ(stats.path("earningsQuarterlyGrowth").path("raw").asDouble(0) * 100);
                cs.setNetIncome(stats.path("netIncomeToCommon").path("raw").asDouble(0));

                // Enterprise Value
                cs.setEnterpriseValue(stats.path("enterpriseValue").path("raw").asDouble(0));
                cs.setEvToRevenue(stats.path("enterpriseToRevenue").path("raw").asDouble(0));
                cs.setEvToEbitda(stats.path("enterpriseToEbitda").path("raw").asDouble(0));

                // Shares & Ownership
                cs.setSharesOutstanding(stats.path("sharesOutstanding").path("raw").asLong(0));
                cs.setInsiderHolding(stats.path("heldPercentInsiders").path("raw").asDouble(0) * 100);
                cs.setInstitutionalHolding(stats.path("heldPercentInstitutions").path("raw").asDouble(0) * 100);
                cs.setBeta(detail.path("beta").path("raw").asDouble(0));
                cs.setBookValue(stats.path("bookValue").path("raw").asDouble(0));

                // Circuit Limits (±20%)
                cs.setUpperCircuit(previousClose * 1.20);
                cs.setLowerCircuit(previousClose * 0.80);

                // Analyst
                cs.setAnalystRating(finData.path("recommendationKey").asText(""));
                cs.setTargetMeanPrice(finData.path("targetMeanPrice").path("raw").asDouble(0));
                cs.setNumberOfAnalystOpinions(
                        finData.path("numberOfAnalystOpinions").path("raw").asInt(0));

                log.info("Successfully fetched company summary for {}", symbol);
                return cs;

            } catch (IOException ex) {
                log.error("IOException on attempt {} for {}", attempts, symbol, ex);
                if (attempts >= maxRetries) {
                    throw new RuntimeException(
                            "Failed to fetch company summary after " + attempts + " attempts", ex);
                }
                try {
                    log.warn("Refreshing auth before retry...");
                    authService.refreshAuthContext();
                } catch (IOException authEx) {
                    log.error("Failed to refresh auth", authEx);
                }
            } catch (Exception ex) {
                log.error("Unexpected error fetching company summary", ex);
                throw new RuntimeException("Failed to fetch company summary", ex);
            }
        }

        throw new RuntimeException(
                "Failed to fetch company summary for " + symbol + " after " + maxRetries + " attempts");
    }

    @Override
    public List<StockQuoteEvent> fetchMarketMovers(MarketMoverType type) {

        String screenerId = switch (type) {
            case GAINERS -> "day_gainers";
            case LOSERS -> "day_losers";
            case MOST_ACTIVE -> "most_actives";
        };


        int attempts = 0;
        int maxRetries = config.getMaxRetries();

        while (attempts < maxRetries) {
            attempts++;
            try {
                final Map<String, String> params = new HashMap<>();
                params.put("scrIds", screenerId);
                params.put("count", "20");

                // Build URL with auth (crumb)
                String url = httpClient.buildUrlWithAuth(
                        config.getMarketMoverEndpoint(),
                        params
                );

                // USE httpClient.executeGet() instead of Jsoup directly
                // This automatically includes cookies + headers from auth context
                String response = httpClient.executeGet(url);

                JsonNode root = MAPPER.readTree(response);

                JsonNode quotes =
                        root.path("finance")
                                .path("result")
                                .get(0)
                                .path("quotes");

                List<StockQuoteEvent> movers = new ArrayList<>();

                for (JsonNode n : quotes) {
                    StockQuoteEvent event = new StockQuoteEvent();
                    event.setSymbol(n.path("symbol").asText());
                    event.setShortName(n.path("shortName").asText(""));
                    event.setLongName(n.path("longName").asText(""));
                    event.setExchange(n.path("exchange").asText(""));
                    event.setMarketState(n.path("marketState").asText("UNKNOWN"));
                    event.setRegularMarketPrice(n.path("regularMarketPrice").asDouble(0.0));
                    event.setRegularMarketChange(n.path("regularMarketChange").asDouble(0.0));
                    event.setRegularMarketChangePercent(n.path("regularMarketChangePercent").asDouble(0.0));
                    event.setRegularMarketTime(n.path("regularMarketTime").asLong(0));
                    movers.add(event);
                }

                return movers;

            } catch (IOException ex) {

                if (attempts >= maxRetries) {
                    throw new RuntimeException("Failed to Market Movers after " + attempts + " attempts", ex);
                }
                // Refresh auth and retry
                try {
                    log.warn("Refreshing auth before retry...");
                    authService.refreshAuthContext();
                } catch (IOException authEx) {
                    log.error("Failed to refresh auth", authEx);
                }
            } catch (Exception ex) {
                log.error("Unexpected error fetching company summary", ex);
                throw new RuntimeException("Failed to fetch company market mover summary", ex);
            }
        }

        throw new RuntimeException("Failed to fetch company summary after " + maxRetries + " attempts");
    }

    private StockQuoteEvent map(JsonNode n) {
        final StockQuoteEvent event = new StockQuoteEvent();

        // ── Core identity ──────────────────────────────────────────
        event.setSymbol(n.path("symbol").asText());
        event.setShortName(n.path("shortName").asText());
        event.setLongName(n.path("longName").asText());
        event.setQuoteType(n.path("quoteType").asText());
        event.setCurrency(n.path("currency").asText());
        event.setExchange(n.path("exchange").asText());
        event.setFullExchangeName(n.path("fullExchangeName").asText());
        event.setExchangeTimezoneName(n.path("exchangeTimezoneName").asText());
        event.setExchangeTimezoneShortName(n.path("exchangeTimezoneShortName").asText());
        event.setMarket(n.path("market").asText());
        event.setMarketState(n.path("marketState").asText("UNKNOWN"));
        event.setQuoteSourceName(n.path("quoteSourceName").asText());
        event.setFinancialCurrency(n.path("financialCurrency").asText());
        event.setRegularMarketDayRange(n.path("regularMarketDayRange").asText());
        event.setFiftyTwoWeekRange(n.path("fiftyTwoWeekRange").asText());

        // ── Regular market prices ──────────────────────────────────
        event.setRegularMarketPrice(n.path("regularMarketPrice").asDouble(0.0));
        event.setRegularMarketDayOpen(n.path("regularMarketOpen").asDouble(0.0));
        event.setRegularMarketDayHigh(n.path("regularMarketDayHigh").asDouble(0.0));
        event.setRegularMarketDayLow(n.path("regularMarketDayLow").asDouble(0.0));
        event.setRegularMarketChange(n.path("regularMarketChange").asDouble(0.0));
        event.setRegularMarketChangePercent(n.path("regularMarketChangePercent").asDouble(0.0));
        event.setRegularMarketPreviousClose(n.path("regularMarketPreviousClose").asDouble(0.0)); //  ADDED
        event.setRegularMarketTime(n.path("regularMarketTime").asLong(0));

        // ── Volume ─────────────────────────────────────────────────
        event.setAverageDailyVolume3Month(n.path("averageDailyVolume3Month").asLong(0)); //  ADDED
        event.setAverageDailyVolume10Day(n.path("averageDailyVolume10Day").asLong(0));   //  ADDED

        // ── 52 Week ────────────────────────────────────────────────
        event.setFiftyTwoWeekHigh(n.path("fiftyTwoWeekHigh").asDouble(0.0));
        event.setFiftyTwoWeekLow(n.path("fiftyTwoWeekLow").asDouble(0.0));
        event.setFiftyTwoWeekChangePercent(n.path("fiftyTwoWeekChangePercent").asDouble(0.0));       //  ADDED
        event.setFiftyTwoWeekHighChange(n.path("fiftyTwoWeekHighChange").asDouble(0.0));             //  ADDED
        event.setFiftyTwoWeekHighChangePercent(n.path("fiftyTwoWeekHighChangePercent").asDouble(0.0)); //  ADDED
        event.setFiftyTwoWeekLowChange(n.path("fiftyTwoWeekLowChange").asDouble(0.0));               //  ADDED
        event.setFiftyTwoWeekLowChangePercent(n.path("fiftyTwoWeekLowChangePercent").asDouble(0.0)); // ADDED

        // ── Moving averages ────────────────────────────────────────
        event.setFiftyDayAverage(n.path("fiftyDayAverage").asDouble(0.0));                               //  ADDED
        event.setFiftyDayAverageChange(n.path("fiftyDayAverageChange").asDouble(0.0));
        event.setFiftyDayAverageChangePercent(n.path("fiftyDayAverageChangePercent").asDouble(0.0));
        event.setTwoHundredDayAverage(n.path("twoHundredDayAverage").asDouble(0.0));                     //  ADDED
        event.setTwoHundredDayAverageChange(n.path("twoHundredDayAverageChange").asDouble(0.0));
        event.setTwoHundredDayAverageChangePercent(n.path("twoHundredDayAverageChangePercent").asDouble(0.0));

        // ── Fundamentals ───────────────────────────────────────────
        event.setTrailingPE(n.path("trailingPE").asDouble(0.0));
        event.setForwardPE(n.path("forwardPE").asDouble(0.0));
        event.setMarketCap(n.path("marketCap").asLong(0));

        // ── Misc ───────────────────────────────────────────────────
        event.setFirstTradeDateMilliseconds(n.path("firstTradeDateMilliseconds").asLong(0));
        event.setGmtOffSetMilliseconds(n.path("gmtOffSetMilliseconds").asLong(0));
        event.setPriceHint(n.path("priceHint").asInt(0));
        event.setSourceInterval(n.path("sourceInterval").asInt(0));
        event.setExchangeDataDelayedBy(n.path("exchangeDataDelayedBy").asInt(0));
        event.setTriggerable(n.path("triggerable").asBoolean(false));
        event.setTradeable(n.path("tradeable").asBoolean(false));
        event.setCryptoTradeable(n.path("cryptoTradeable").asBoolean(false));
        event.setHasPrePostMarketData(n.path("hasPrePostMarketData").asBoolean(false));
        event.setEsgPopulated(n.path("esgPopulated").asBoolean(false));
        event.setMessageBoardId(n.path("messageBoardId").asText());
        event.setCustomPriceAlertConfidence(n.path("customPriceAlertConfidence").asText());
        event.setRegion(n.path("region").asText());
        event.setLanguage(n.path("language").asText());
        event.setTypeDisp(n.path("typeDisp").asText());

        return event;
    }

    /**
     * Formats raw market cap (in INR) to "₹X.XX L.Cr" label.
     * 1 Lakh Crore = 1,00,000 Cr = 1,000,000,000,000
     */
    private String formatToLakhCrore(double raw) {
        double lakhCrore = raw / 1_00_00_00_00_000.0;
        return String.format("%.2f L.Cr", lakhCrore);
    }

    private String resolveMarketCapCategory(double marketCapRaw) {
        double crore = marketCapRaw / 1_00_00_000.0;
        if (crore >= 20_000) return "Large Cap";
        if (crore >= 5_000)  return "Mid Cap";
        return "Small Cap";
    }
}
