package net.luversof.web.dynamiccrud2.admin.controller;

/**
 * 관리 화면 위치 표시(Breadcrumbs) 한 칸. href 가 null 이면 링크 없이 글자만(지금 화면 · 따로 화면이 없는 메인 메뉴).
 *
 * @param label 보일 이름
 * @param href 이동할 주소(없으면 null)
 */
public record Crumb(String label, String href) {}
