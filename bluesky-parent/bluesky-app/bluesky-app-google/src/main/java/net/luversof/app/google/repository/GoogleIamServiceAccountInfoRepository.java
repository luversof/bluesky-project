package net.luversof.app.google.repository;

import java.util.Optional;
import java.util.UUID;
import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;
import org.springframework.data.repository.CrudRepository;

public interface GoogleIamServiceAccountInfoRepository
        extends CrudRepository<GoogleIamServiceAccountInfo, UUID> {
    Optional<GoogleIamServiceAccountInfo> findByUserId(UUID userId);
}
