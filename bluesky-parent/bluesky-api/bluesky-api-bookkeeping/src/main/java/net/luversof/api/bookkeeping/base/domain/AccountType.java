package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 계좌 유형 정의
 * 유저 별로 따로 정의하여 사용할 수 있음
 */
@Data
@Entity
@Table(indexes = { @Index(name = "IDX_accountType_bookkeepingId", columnList = "bookkeeping_id") })
public class AccountType {

	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private AccountTypeCode code;
	
	private String name;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}
