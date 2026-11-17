package net.luversof.web.dynamiccrud2.setting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.setting.domain.MainMenu;

public interface MainMenuRepository extends ListCrudRepository<MainMenu, UUID> {

  List<MainMenu> findByProjectIdOrderByDisplayOrderAscNameAsc(UUID projectId);
}
