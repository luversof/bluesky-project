package net.luversof.api.bookkeeping.domain;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Null;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;

@Table("EntryType")
public class EntryType {

  @Null(groups = Create.class)
  @Id
  private UUID id;

  @Column("bookkeeping_id")
  private UUID bookkeepingId;

  private EntryTypeCode code;

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

  public EntryTypeCode getCode() {
    return code;
  }

  public void setCode(EntryTypeCode code) {
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
    EntryType entryType = (EntryType) o;
    return Objects.equals(id, entryType.id)
        && Objects.equals(bookkeepingId, entryType.bookkeepingId)
        && code == entryType.code
        && Objects.equals(name, entryType.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookkeepingId, code, name);
  }

  @Override
  public String toString() {
    return "EntryType{"
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
