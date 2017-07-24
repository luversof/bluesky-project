package net.luversof.blog.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.Data;

/**
 * blog 정보
 * @author luver
 *
 */
@Data
@Entity
@Table(indexes = @Index(name = "IDX_Blog_userId", columnList = "user_id", unique = true) )
public class Blog {

	@Id
//	@GeneratedValue(generator = "system-uuid")
//	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@GeneratedValue(generator = "uuid-gen")
	@GenericGenerator(name = "uuid-gen", strategy = "uuid2")
	@Column(length = 16)
	private UUID id;

	@Column(name = "user_id", length = 16)
	private UUID userId;

}
