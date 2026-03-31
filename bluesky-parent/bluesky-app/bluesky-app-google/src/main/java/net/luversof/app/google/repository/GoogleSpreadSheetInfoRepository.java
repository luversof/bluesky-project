package net.luversof.app.google.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import net.luversof.app.google.domain.GoogleSpreadSheetInfo;

public interface GoogleSpreadSheetInfoRepository
        extends CrudRepository<GoogleSpreadSheetInfo, UUID> {

    List<GoogleSpreadSheetInfo> findByGoogleIamServiceAccountInfoId(
            UUID googleIamServiceAccountInfoId);

    Optional<GoogleSpreadSheetInfo> findByGoogleIamServiceAccountInfoIdAndType(
            UUID googleIamServiceAccountInfoId, GoogleSpreadSheetInfoType type);
}
