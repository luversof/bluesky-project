package net.luversof.asset.repository;

import java.util.List;

import net.luversof.asset.domain.Asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, QueryDslPredicateExecutor<Asset> {
	List<Asset> findByUsername(String username);
}
