package com.project.sentic.infra.ai;

import com.project.sentic.infra.ai.dto.CharacterInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GPT-4o 시스템 프롬프트 빌더
 *
 * 등장인물 1명: 단독 캐릭터 대화
 * 등장인물 2명: [CharacterName]: 형식으로 멀티 캐릭터 대화
 */
@Component
public class PromptBuilder {

    public String build(PromptContext context) {
        validate(context.characters());

        StringBuilder sb = new StringBuilder();

        if (context.characters().size() == 1) {
            buildSingleCharacter(sb, context);
        } else {
            buildMultiCharacter(sb, context);
        }

        return sb.toString();
    }

    // ── 단독 캐릭터 ──────────────────────────────────────────

    private void buildSingleCharacter(StringBuilder sb, PromptContext ctx) {
        CharacterInfo ch = ctx.characters().get(0);

        sb.append("You are ").append(ch.name())
          .append(", playing a role in the following scenario.\n\n");

        sb.append("## CHARACTER\n");
        sb.append("Name: ").append(ch.name()).append("\n");
        sb.append("Personality / Role: ").append(ch.personality()).append("\n\n");

        appendSituation(sb, ctx);
        appendMemoryBank(sb, ctx.memoryBank());
        appendDifficulty(sb, ctx.difficulty());

        sb.append("\n## RULES — NEVER violate these\n");
        sb.append("1. You are ALWAYS ").append(ch.name()).append(". Never break character.\n");
        sb.append("2. Never reveal that you are an AI or a language model.\n");
        sb.append("3. Respond ONLY in English.\n");
        sb.append("4. If the user speaks Korean or tries to leave the scenario, ")
          .append("respond in English and gently steer back.\n");
        sb.append("5. Keep all responses relevant to the current situation.");
    }

    // ── 멀티 캐릭터 (최대 2명) ──────────────────────────────────

    private void buildMultiCharacter(StringBuilder sb, PromptContext ctx) {
        CharacterInfo ch1 = ctx.characters().get(0);
        CharacterInfo ch2 = ctx.characters().get(1);

        sb.append("You are playing TWO characters. Alternate naturally between them.\n");
        sb.append("FORMAT: Every line MUST start with [CharacterName]: followed by the dialogue.\n\n");

        sb.append("## CHARACTERS\n");
        sb.append("1. ").append(ch1.name()).append(" — ").append(ch1.personality()).append("\n");
        sb.append("2. ").append(ch2.name()).append(" — ").append(ch2.personality()).append("\n\n");

        appendSituation(sb, ctx);
        appendMemoryBank(sb, ctx.memoryBank());
        appendDifficulty(sb, ctx.difficulty());

        sb.append("\n## RULES — NEVER violate these\n");
        sb.append("1. EVERY line MUST begin with [").append(ch1.name())
          .append("]: or [").append(ch2.name()).append("]:.\n");
        sb.append("2. Both characters must always stay true to their personalities.\n");
        sb.append("3. Never reveal that you are an AI or a language model.\n");
        sb.append("4. Respond ONLY in English.\n");
        sb.append("5. If the user speaks Korean or tries to leave the scenario, ")
          .append("respond in character and redirect to the situation.");
    }

    // ── 공통 섹션 ──────────────────────────────────────────────

    private void appendMemoryBank(StringBuilder sb, String memoryBank) {
        if (memoryBank == null || memoryBank.isBlank()) return;
        sb.append("## MEMORY BANK\n");
        sb.append("Use this information to maintain continuity with the user:\n");
        sb.append(memoryBank).append("\n\n");
    }

    private void appendSituation(StringBuilder sb, PromptContext ctx) {
        sb.append("## SITUATION: ").append(ctx.roomName()).append("\n");
        sb.append(ctx.situation()).append("\n\n");
    }

    private void appendDifficulty(StringBuilder sb, String difficulty) {
        String level = difficulty != null ? difficulty.toUpperCase() : "INTERMEDIATE";
        sb.append("## LANGUAGE LEVEL: ").append(level).append("\n");
        sb.append(switch (level) {
            case "BEGINNER" ->
                    "- Use short, simple sentences (under 10 words when possible).\n" +
                    "- Choose basic, everyday vocabulary only.\n" +
                    "- Avoid idioms, slang, and complex grammar.\n" +
                    "- Repeat key words to help the learner follow along.";
            case "ADVANCED" ->
                    "- Use natural, fluent English including slang and idioms.\n" +
                    "- Contractions, phrasal verbs, and colloquialisms are encouraged.\n" +
                    "- Vary sentence structure freely and use complex grammar naturally.";
            default ->
                    "- Use natural everyday conversational English.\n" +
                    "- Mix simple and moderate vocabulary.\n" +
                    "- Occasional common idioms are fine; avoid heavy slang.";
        });
        sb.append("\n");
    }

    private void validate(List<CharacterInfo> characters) {
        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("등장인물은 최소 1명이어야 합니다.");
        }
        if (characters.size() > 2) {
            throw new IllegalArgumentException("등장인물은 최대 2명입니다.");
        }
    }

    // ── PromptContext ──────────────────────────────────────────

    public record PromptContext(
            String roomName,
            String situation,
            List<CharacterInfo> characters,
            String difficulty,         // "BEGINNER" | "INTERMEDIATE" | "ADVANCED"
            String memoryBank          // nullable — rooms.memory_bank JSON
    ) {}
}
