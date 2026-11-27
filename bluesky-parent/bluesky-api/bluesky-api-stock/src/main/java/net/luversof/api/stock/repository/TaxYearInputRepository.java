package net.luversof.api.stock.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.TaxYearInput;

public interface TaxYearInputRepository extends CrudRepository<TaxYearInput, UUID> {

  /** uk_taxYearInput_userId_year 인덱스를 탄다. */
  Optional<TaxYearInput> findByUserIdAndYear(UUID userId, int year);
}
