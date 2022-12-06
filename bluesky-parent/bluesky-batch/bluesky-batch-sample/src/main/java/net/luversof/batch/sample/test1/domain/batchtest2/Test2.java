package net.luversof.batch.sample.test1.domain.batchtest2;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Test2 {

	@Id
	private long id;
	
	private String testValue;
}
