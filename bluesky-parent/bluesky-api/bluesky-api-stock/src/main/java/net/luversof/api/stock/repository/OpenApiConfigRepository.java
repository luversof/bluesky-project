package net.luversof.api.stock.repository;

import java.util.Optional;
import java.util.UUID;
import net.luversof.api.stock.domain.OpenApiConfig;
import org.springframework.data.repository.CrudRepository;

public interface OpenApiConfigRepository extends CrudRepository<OpenApiConfig, UUID> {
    Optional<OpenApiConfig> findByProvider(String provider);
}
