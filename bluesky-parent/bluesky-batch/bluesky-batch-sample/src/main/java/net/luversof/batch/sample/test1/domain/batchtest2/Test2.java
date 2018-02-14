package net.luversof.batch.sample.test1.domain.batchtest2;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Test2 {

	@Id
	private long id;
	
	private String testValue;
}
