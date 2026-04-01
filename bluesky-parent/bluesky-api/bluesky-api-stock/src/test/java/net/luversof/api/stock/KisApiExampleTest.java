package net.luversof.api.stock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.service.kis.KisAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
public class KisApiExampleTest {

    @Autowired private KisAuthService kisAuthService;

    @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void testFetchAndSaveKisDailyPrice() {
        OpenApiConfig config;
        try {
            config = kisAuthService.getValidConfig();
        } catch (Exception e) {
            System.out.println("KIS API is not configured. Skipping test: " + e.getMessage());
            return;
        }

        String baseUrl = "https://openapi.koreainvestment.com:9443";
        String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + config.getAccessToken());
        headers.set("appkey", config.getAppKey());
        headers.set("appsecret", config.getAppSecret());
        headers.set("tr_id", "FHKST03010100");

        String url =
                UriComponentsBuilder.fromUriString(baseUrl + path)
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", "005930")
                        .queryParam("FID_INPUT_DATE_1", "20230101")
                        .queryParam("FID_INPUT_DATE_2", "20230110")
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build()
                        .toUriString();

        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<KisDailyPriceResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, KisDailyPriceResponse.class);
        assertNotNull(response.getBody());
        List<KisDailyPriceItem> items = response.getBody().getOutput2();

        if (items == null || items.isEmpty()) {
            System.out.println("No chart data retrieved.");
            return;
        }

        for (KisDailyPriceItem item : items) {
            if (item.getStck_bsop_date() == null || item.getStck_bsop_date().isEmpty()) {
                continue;
            }

            LocalDate tradeDate =
                    LocalDate.parse(
                            item.getStck_bsop_date(), DateTimeFormatter.ofPattern("yyyyMMdd"));

            StockPriceHistory history = new StockPriceHistory();
            history.setStockItemId(java.util.UUID.randomUUID());
            history.setTradeDate(tradeDate);
            history.setOpenPrice(new BigDecimal(item.getStck_oprc()));
            history.setHighPrice(new BigDecimal(item.getStck_hgpr()));
            history.setLowPrice(new BigDecimal(item.getStck_lwpr()));
            history.setClosePrice(new BigDecimal(item.getStck_clpr()));
            history.setVolume(Long.parseLong(item.getAcml_vol()));

            System.out.println(
                    "Data to save: " + history.getTradeDate() + " - " + history.getClosePrice());
        }
    }

    public static class KisDailyPriceResponse {
        private String rt_cd;
        private String msg_cd;
        private String msg1;
        private List<KisDailyPriceItem> output2;

        public String getRt_cd() {
            return rt_cd;
        }

        public void setRt_cd(String rt_cd) {
            this.rt_cd = rt_cd;
        }

        public String getMsg_cd() {
            return msg_cd;
        }

        public void setMsg_cd(String msg_cd) {
            this.msg_cd = msg_cd;
        }

        public String getMsg1() {
            return msg1;
        }

        public void setMsg1(String msg1) {
            this.msg1 = msg1;
        }

        public List<KisDailyPriceItem> getOutput2() {
            return output2;
        }

        public void setOutput2(List<KisDailyPriceItem> output2) {
            this.output2 = output2;
        }
    }

    public static class KisDailyPriceItem {
        private String stck_bsop_date;
        private String stck_clpr;
        private String stck_oprc;
        private String stck_hgpr;
        private String stck_lwpr;
        private String acml_vol;

        public String getStck_bsop_date() {
            return stck_bsop_date;
        }

        public void setStck_bsop_date(String stck_bsop_date) {
            this.stck_bsop_date = stck_bsop_date;
        }

        public String getStck_clpr() {
            return stck_clpr;
        }

        public void setStck_clpr(String stck_clpr) {
            this.stck_clpr = stck_clpr;
        }

        public String getStck_oprc() {
            return stck_oprc;
        }

        public void setStck_oprc(String stck_oprc) {
            this.stck_oprc = stck_oprc;
        }

        public String getStck_hgpr() {
            return stck_hgpr;
        }

        public void setStck_hgpr(String stck_hgpr) {
            this.stck_hgpr = stck_hgpr;
        }

        public String getStck_lwpr() {
            return stck_lwpr;
        }

        public void setStck_lwpr(String stck_lwpr) {
            this.stck_lwpr = stck_lwpr;
        }

        public String getAcml_vol() {
            return acml_vol;
        }

        public void setAcml_vol(String acml_vol) {
            this.acml_vol = acml_vol;
        }
    }
}
