package net.luversof.bbs.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

@Data
@Entity
@Table(name = "BbsArticle")
public class Article {

	@Id
	@GeneratedValue
	@NotNull(groups = { Get.class })
	private long id;

	@ManyToOne
	@JoinColumn(name = "bbs_id", foreignKey = @ForeignKey(name = "FK_article_bbsId") )
	private Bbs bbs;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String content;

	@Column(updatable = false)
	@CreatedDate
	private LocalDateTime createdDate;

	@Column(updatable = false)
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}