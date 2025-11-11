package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * 주식 현재가 정보
 */
@Data
@Table("StockPrice")
public class StockPrice {

    @Null(groups = Create.class)
    @NotNull(groups = { Update.class, Delete.class })
    @Id
    private UUID id;

    @NotNull(groups = { Create.class, Update.class })
    @Column("stockItem_id")
    private UUID stockItemId;

    @NotNull(groups = { Create.class, Update.class })
    private BigDecimal price;

    @LastModifiedDate
    @Column("updatedDate")
    private Instant updatedDate;

    public interface Create {
    }

    public interface Update {
    }

    public interface Delete {
    }
}
