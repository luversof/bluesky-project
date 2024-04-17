package net.luversof.api.bookkeeping.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.domain.Account;

public interface AccountRepository extends JpaRepository<Account, UUID>{

}
