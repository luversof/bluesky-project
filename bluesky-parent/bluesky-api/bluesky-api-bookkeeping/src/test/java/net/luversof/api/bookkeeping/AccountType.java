package net.luversof.api.bookkeeping;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_accountType_bookkeepingId", columnList = "bookkeeping_id") })
public class AccountType {


	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@NotBlank(groups = { Create.class, Update.class })
	@Column(name = "bookkeeping_id", nullable = false)
	private UUID bookkeepingId;
	
	@Enumerated(EnumType.STRING)
	private AccountTypeCode accountTypeCode;
	
	private String name;
	
	public static interface Create {
	}

	public static interface CreateParam {
	}

	public static interface Update {
	}

	public static interface Delete {
	}
	
}
