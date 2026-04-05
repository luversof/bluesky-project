package net.luversof.app.google.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** user당 1개씩 존재하는 Google IAM Service Account 정보 */
@Table("GoogleIamServiceAccountInfo")
public class GoogleIamServiceAccountInfo {

  @Id
  @Column("id")
  private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("keyStr")
  private String keyStr;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getKeyStr() {
    return keyStr;
  }

  public void setKeyStr(String keyStr) {
    this.keyStr = keyStr;
  }
}
