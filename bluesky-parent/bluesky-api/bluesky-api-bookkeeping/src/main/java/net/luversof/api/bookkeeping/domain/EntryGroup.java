//package net.luversof.api.bookkeeping.domain;
//
//import java.util.UUID;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Index;
//import jakarta.persistence.Table;
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//import net.luversof.api.bookkeeping.constant.EntryGroupType;
//
///**
// * 분류 항목
// * 
// * @author bluesky
// *
// */
//@Data
//@Entity
//@Table(indexes = { @Index(name = "IDX_entryGroup_bookkeepingId", columnList = "bookkeeping_id") })
//public class EntryGroup {
//	
//	@NotBlank(groups = { Update.class, Delete.class })
//	@Id
//	@GeneratedValue(strategy = GenerationType.UUID)
//	private UUID id;
//
//	@Column(name = "bookkeeping_id", nullable = false)
//	private UUID bookkeepingId;
//	
//	@NotBlank(groups = { Create.class, Update.class })
//	@Enumerated(EnumType.STRING)
//	private EntryGroupType entryGroupType;
//	
//	@NotBlank(groups = { Create.class, Update.class })
//	private String name;
//	
//
//	public interface Create {
//	}
//
//	public interface Update {
//	}
//
//	public interface Delete {
//	}
//}
