package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.Asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, QueryDslPredicateExecutor<Asset> {
	List<Asset> findByUsername(String username);
}
