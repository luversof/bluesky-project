package net.luversof.api.bookkeeping.base.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_account_bookkeepingId", columnList = "bookkeeping_id") })
public class Account {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@NotNull(groups = { Update.class, Delete.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Column(name = "accountType_id", nullable = false)
	private UUID accountTypeId;
	
	private String name;
	
	public interface Create {}
	public interface Update {}
	public interface Delete {}

}
