package net.luversof.web.dynamiccrud.support;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DynamicCrudSettingTransactionHandler {

	@Transactional(transactionManager = "dynamicCrudSettingTransactionManager", isolation = Isolation.READ_UNCOMMITTED)
	public <T extends Object> T runInReadUncommittedTransaction(Supplier<T> supplier) {
		return supplier.get();
	}
	
}
