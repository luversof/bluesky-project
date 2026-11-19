package net.luversof.web.dynamiccrud2.repository;

import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.SettingLog;

/** 설정 변경 이력. 화면 조회(범위 · 거르기 · 쪽)는 {@link SettingLogQueries} */
public interface SettingLogRepository
    extends ListCrudRepository<SettingLog, UUID>, SettingLogQueries {}
