package net.luversof.app.google.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.app.google.constant.TestConstant;

public class StockGoogleSheetServiceTest implements GeneralTest {

    @Autowired private StockGoogleSheetService stockGoogleSheetService;

    @Test
    void getGoogleSheetTradeListTest() {
        var googleSheetTradeList =
                stockGoogleSheetService.getGoogleSheetTradeList(TestConstant.USER_ID);
        assertThat(googleSheetTradeList).isNotNull();
        assertThat(googleSheetTradeList.size()).isGreaterThan(0);
    }
}
