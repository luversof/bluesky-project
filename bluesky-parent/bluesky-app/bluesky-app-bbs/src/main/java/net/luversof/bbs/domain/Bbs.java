package net.luversof.bbs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_bbs_bbsId", columnList = "bbsId", unique = true), @Index(name = "UK_bbs_alias", columnList = "alias", unique = true) })
public class Bbs {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(length = 36, nullable = false)
	private String bbsId;
	
	@Column(length = 15, nullable = false)
	private String alias;
	
	private boolean isBbsActivated;
	
	private boolean isArticleActivated;
	
	private boolean isReplyActivated;
	
	private boolean isCommentActivated;
	
}
