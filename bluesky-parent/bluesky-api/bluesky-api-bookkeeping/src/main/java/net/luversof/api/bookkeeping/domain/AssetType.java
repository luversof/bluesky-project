package net.luversof.api.bookkeeping.domain;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;

/** 계좌 유형 정의 유저 별로 따로 정의하여 사용할 수 있음 */
@Table(name = "AssetType")
public class AssetType {

  @Id private UUID id;

  @NotNull(groups = {Create.class, Update.class})
  @Column("bookkeeping_id")
  private UUID bookkeepingId;

  @NotNull(groups = {Create.class, Update.class})
  // @Convert(converter = AssetTypeCodeConverter.class)
  private AssetTypeCode code;

  @NotBlank(groups = {Create.class, Update.class})
  private String name;

  public interface Create {}

  public interface Update {}

  public interface Delete {}

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

  public AssetTypeCode getCode() {
    return code;
  }

  public void setCode(AssetTypeCode code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssetType assetType = (AssetType) o;
    return Objects.equals(id, assetType.id)
        && Objects.equals(bookkeepingId, assetType.bookkeepingId)
        && code == assetType.code
        && Objects.equals(name, assetType.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookkeepingId, code, name);
  }

  @Override
  public String toString() {
    return "AssetType{"
        + "id="
        + id
        + ", bookkeepingId="
        + bookkeepingId
        + ", code="
        + code
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
