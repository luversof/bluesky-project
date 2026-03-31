package net.luversof.api.bookkeeping.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** entry는 대상 account에 대한 credit, debit 정보 중 하나를 저장 다중 기록을 하며 credit + debit의 총 합은 무조건 0 */
@Table("Entry")
public class Entry {

    @Null(groups = Create.class)
    @NotNull(groups = {Update.class, Delete.class})
    @Id
    // @GeneratedValue(strategy = GenerationType.UUID)
    // @UuidGenerator(style = Style.TIME)
    private UUID id;

    @NotNull(groups = {Create.class, Update.class})
    @Column("bookkeeping_id")
    private UUID bookkeepingId;

    @Column("entryType_id")
    private UUID entryTypeId;

    @NotNull(groups = {Create.class, Update.class})
    @Column("incomeAsset_id")
    private UUID incomeAssetId;

    @NotNull(groups = {Create.class, Update.class})
    @Column("outgoingAsset_id")
    private UUID outgoingAssetId;

    @NotNull(groups = {Create.class, Update.class})
    @Column("entryDate")
    private Instant entryDate;

    @NotNull(groups = {Create.class, Update.class})
    private BigDecimal amount;

    @Column("extraData")
    private Map<String, Object> extraData;

    public interface Create {}

    public interface Update {}

    public interface Delete {}

    public static class EntryExtraData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String memo;

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntryExtraData that = (EntryExtraData) o;
            return Objects.equals(memo, that.memo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(memo);
        }

        @Override
        public String toString() {
            return "EntryExtraData{" + "memo='" + memo + '\'' + '}';
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookkeepingId() {
        return bookkeepingId;
    }

    public void setBookkeepingId(UUID bookkeepingId) {
        this.bookkeepingId = bookkeepingId;
    }

    public UUID getEntryTypeId() {
        return entryTypeId;
    }

    public void setEntryTypeId(UUID entryTypeId) {
        this.entryTypeId = entryTypeId;
    }

    public UUID getIncomeAssetId() {
        return incomeAssetId;
    }

    public void setIncomeAssetId(UUID incomeAssetId) {
        this.incomeAssetId = incomeAssetId;
    }

    public UUID getOutgoingAssetId() {
        return outgoingAssetId;
    }

    public void setOutgoingAssetId(UUID outgoingAssetId) {
        this.outgoingAssetId = outgoingAssetId;
    }

    public Instant getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(Instant entryDate) {
        this.entryDate = entryDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Objects.equals(id, entry.id)
                && Objects.equals(bookkeepingId, entry.bookkeepingId)
                && Objects.equals(entryTypeId, entry.entryTypeId)
                && Objects.equals(incomeAssetId, entry.incomeAssetId)
                && Objects.equals(outgoingAssetId, entry.outgoingAssetId)
                && Objects.equals(entryDate, entry.entryDate)
                && Objects.equals(amount, entry.amount)
                && Objects.equals(extraData, entry.extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                bookkeepingId,
                entryTypeId,
                incomeAssetId,
                outgoingAssetId,
                entryDate,
                amount,
                extraData);
    }

    @Override
    public String toString() {
        return "Entry{"
                + "id="
                + id
                + ", bookkeepingId="
                + bookkeepingId
                + ", entryTypeId="
                + entryTypeId
                + ", incomeAssetId="
                + incomeAssetId
                + ", outgoingAssetId="
                + outgoingAssetId
                + ", entryDate="
                + entryDate
                + ", amount="
                + amount
                + ", extraData="
                + extraData
                + '}';
    }
}
