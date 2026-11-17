package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.MainMenu;

public interface MainMenuRepository extends ListCrudRepository<MainMenu, UUID> {

  List<MainMenu> findByProjectIdOrderByDisplayOrderAscNameAsc(UUID projectId);

  Optional<MainMenu> findByProjectIdAndCode(UUID projectId, String code);
}
