package net.luversof.blog.domain.mysql;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;

import lombok.Data;

/**
 * blog 정보
 * @author luver
 *
 */
@Data
@Entity
@Audited
@Table(indexes = @Index(name = "IDX_Blog_userId", columnList = "user_id", unique = true) )
public class Blog {

	@Id
	@GeneratedValue(generator = "uuid-gen")
	@GenericGenerator(name = "uuid-gen", strategy = "uuid2")
	@Column(length = 16)
	private UUID id;

	@Column(name = "user_id", length = 16, nullable = false)
	private UUID userId;
	
	@CreatedDate
	private LocalDateTime createdDate;

}
