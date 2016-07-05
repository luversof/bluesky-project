package net.luversof.bbs.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_Bbs_aliasName", columnList = "aliasName") )
public class Bbs {

	@Id
	@GeneratedValue
	private long id;
	
	private String aliasName;
	
}
