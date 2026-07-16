package net.luversof.web.dynamiccrud.setting.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.View;

/** 동일 기능처리를 여러 path로 처리하기 위해 구성한 interface path 별 권한 부여 목적을 위해 분기 */
public interface SettingFragmentControllerInterface {

  /** 목록 fragment */
  String list(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      Pageable pageable,
      Map<String, String> paramMap,
      Model model);

  /** modal form fragment */
  String modalForm(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      String modalMode,
      Model model);

  /** modal 생성/수정 요청 */
  void createModal(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      String modalMode,
      Map<String, String> dataMap,
      Model model);

  /** modal 삭제 요청 */
  void deleteModal(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      String modalMode,
      MultiValueMap<String, String> dataMap,
      Model model);

  /** 대량 import 처리를 위한 modal form */
  String modalBulkForm(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      String modalMode,
      Model model);

  /** 대량 import 요청 */
  void importModalBulk(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      String modalMode,
      Map<String, String> dataMap,
      Model model);

  /** 엑셀 다운로드 */
  View excel(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      Map<String, String> paramMap,
      Pageable pageable,
      Model model);

  /** 벌크 쿼리 실행 modal form (ROLE_MASTER/ROLE_ADMIN 전용) */
  String bulkQueryForm(
      String adminProjectId, String projectId, String mainMenuId, String subMenuId, Model model);

  /** 벌크 쿼리 실행 요청 (ROLE_MASTER/ROLE_ADMIN 전용). 결과 fragment를 반환. */
  String executeBulkQuery(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      Map<String, String> dataMap,
      Model model);
}
