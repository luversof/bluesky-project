package net.luversof.batch.sample.test1.domain.batchtest1;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Test1 {

	@Id
	private long id;
	
	private String testValue;
}
