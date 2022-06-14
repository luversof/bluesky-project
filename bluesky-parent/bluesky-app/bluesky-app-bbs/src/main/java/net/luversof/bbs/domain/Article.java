package net.luversof.bbs.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

@Data
@Entity
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class })
	private long id;
	
	@Column(name = "user_id")
	private long userId;

	@ManyToOne
	@JoinColumn(name = "bbs_id", foreignKey = @ForeignKey(name = "FK_article_bbsId"))
	private Bbs bbs;

	@NotBlank(groups = { Save.class, Modify.class })
	private String title;

	@NotBlank(groups = { Save.class, Modify.class })
	private String content;

	@Column(updatable = false)
	@CreatedDate
	private LocalDateTime createdDate;

	@Column(updatable = false)
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	public interface Get {
	}

    public interface Save {
	}

    public interface Modify {
	}
}