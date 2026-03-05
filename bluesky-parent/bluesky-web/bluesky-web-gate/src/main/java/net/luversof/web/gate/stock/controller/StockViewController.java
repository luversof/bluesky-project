package net.luversof.web.gate.stock.controller;

import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

        private AccountClient accountClient;

        private StockItemClient stockItemClient;

        @Autowired
        public void setAccountClient(AccountClient accountClient) {
                this.accountClient = accountClient;
        }

        @Autowired
        public void setStockItemClient(StockItemClient stockItemClient) {
                this.stockItemClient = stockItemClient;
        }

        private String getLoginRedirectUrl(HttpServletRequest request) {
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(scheme).append("://").append(serverName);
                if (serverPort != 80 && serverPort != 443) {
                        urlBuilder.append(":").append(serverPort);
                }
                urlBuilder.append(request.getRequestURI());
                
                if (request.getQueryString() != null) {
                        urlBuilder.append("?").append(request.getQueryString());
                }
                
                String encodedUrl = URLEncoder.encode(urlBuilder.toString(), StandardCharsets.UTF_8);
                return "redirect:/login?redirectUrl=" + encodedUrl;
        }

        private boolean isNotAuthenticated() {
                return UserUtil.getUserId() == null;
        }

        @BlueskyPreAuthorize
        @GetMapping
        public String index(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                
                UUID userId = UserUtil.getUserId();
                var accounts = accountClient.getAccountsByUserId(userId);
                model.addAttribute("accounts", accounts);
                model.addAttribute("userId", userId);

                var stockItems = stockItemClient.getStockItems();
                model.addAttribute("stockItems", stockItems);
                return "stock/dashboard";
        }

        @BlueskyPreAuthorize
        @GetMapping("/analytics")
        public String analyticsPage(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                return "stock/analytics";
        }

        @BlueskyPreAuthorize
        @GetMapping("/dashboard")
        public String dashboard(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                return "redirect:/stock"; 
        }

        @BlueskyPreAuthorize
        @GetMapping("/activity")
        public String activityPage(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                return "stock/activity";
        }

        @BlueskyPreAuthorize
        @GetMapping("/dividend")
        public String dividendPage(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                return "stock/dividend";
        }

        @BlueskyPreAuthorize
        @GetMapping("/trade")
        public String tradePage(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                
                UUID userId = UserUtil.getUserId();
                var accounts = accountClient.getAccountsByUserId(userId);
                model.addAttribute("accounts", accounts);

                var stockItems = stockItemClient.getStockItems();
                model.addAttribute("stockItems", stockItems);
                return "stock/trade";
        }

        @BlueskyPreAuthorize
        @GetMapping("/asset-growth")
        public String assetGrowthPage(HttpServletRequest request, Model model) {
                if (isNotAuthenticated()) {
                        return getLoginRedirectUrl(request);
                }
                return "stock/assetGrowth";
        }
}
