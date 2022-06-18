package net.luversof.bbs.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

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
