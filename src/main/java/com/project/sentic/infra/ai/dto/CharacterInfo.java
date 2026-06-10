package com.project.sentic.infra.ai.dto;

/**
 * 방 등장인물 정보 — rooms.characters JSON 컬럼과 1:1 매핑
 * 예: {"name": "Sarah", "personality": "친절하고 빠른 서비스"}
 */
public record CharacterInfo(String name, String personality) {}
