package net.luversof.api.stock.service.kis.dto;

public class KisDailyPriceItem {
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
