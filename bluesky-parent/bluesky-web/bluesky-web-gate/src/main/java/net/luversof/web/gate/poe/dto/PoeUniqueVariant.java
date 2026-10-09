package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 고유 아이템의 변형 하나 (임프레션스 물리/화염/…, 희망의 실 고리 크기 등). nameKo 는 확실할 때만 채워지고 아니면 null. */
public record PoeUniqueVariant(
    int index,
    String name,
    String nameKo,
    List<String> implicits,
    List<String> implicitsKo,
    List<String> explicits,
    List<String> explicitsKo,
    // 옵션 줄별 인게임 리마인더(10-04 C114)
    List<List<String>> explicitsReminders,
    List<List<String>> explicitsRemindersKo) {}
