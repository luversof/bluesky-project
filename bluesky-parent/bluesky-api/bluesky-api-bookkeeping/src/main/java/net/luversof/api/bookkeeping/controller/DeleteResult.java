package net.luversof.api.bookkeeping.controller;

/** 자산 · 분류 삭제 결과. 내역이 있어 숨기기만 했으면 deleted=false. */
public record DeleteResult(boolean deleted) {}
