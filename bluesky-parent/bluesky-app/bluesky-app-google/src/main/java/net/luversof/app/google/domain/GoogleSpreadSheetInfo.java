package net.luversof.app.google.domain;

import java.util.UUID;
import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GoogleSpreadSheetInfo")
public class GoogleSpreadSheetInfo {

    @Id
    @Column("id")
    private UUID id;

    @Column("googleIamServiceAccountInfo_id")
    private UUID googleIamServiceAccountInfoId;

    @Column private GoogleSpreadSheetInfoType type;

    @Column("spreadsheetId")
    private String spreadsheetId;

    private String range;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGoogleIamServiceAccountInfoId() {
        return googleIamServiceAccountInfoId;
    }

    public void setGoogleIamServiceAccountInfoId(UUID googleIamServiceAccountInfoId) {
        this.googleIamServiceAccountInfoId = googleIamServiceAccountInfoId;
    }

    public GoogleSpreadSheetInfoType getType() {
        return type;
    }

    public void setType(GoogleSpreadSheetInfoType type) {
        this.type = type;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }
}
