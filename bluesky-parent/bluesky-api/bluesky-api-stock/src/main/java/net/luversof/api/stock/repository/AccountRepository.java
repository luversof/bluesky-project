package net.luversof.api.stock.repository;

import java.util.List;
import java.util.UUID;
import net.luversof.api.stock.domain.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, UUID> {

    List<Account> findByIdIn(List<UUID> idList);

    List<Account> findByUserId(UUID userId);
}
