package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.EntryTransaction;

public interface EntryTransactionRepository extends JpaRepository<EntryTransaction, UUID> {

}
