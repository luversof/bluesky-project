package net.luversof.bbs.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bbs_userId", columnList = "user_id") )
public class Bbs {

	@Id
	@GeneratedValue
	private long id;

	@Column(name = "user_id")
	private long userId;
}
