package com.project.sentic.infra.ai;

public class VoiceMapper {

    /**
     * 캐릭터 성격에 따라 TTS 목소리 매칭
     *
     * alloy   - 중성적, 차분한 (기본값)
     * echo    - 낮고 무거운 (진지한 캐릭터)
     * fable   - 밝고 활기찬 (밝은 캐릭터)
     * onyx    - 깊고 진지한 (까칠한/무뚝뚝한 캐릭터)
     * nova    - 따뜻하고 친근한 (친절한 캐릭터)
     * shimmer - 부드럽고 섬세한 (조용한/수줍은 캐릭터)
     */

    public static String mapVoice(String personality) {
        if (personality == null || personality.isBlank()) {
            return "alloy";
        }

        String lower = personality.toLowerCase();

        // 까칠/화남/무뚝뚝/엄격/진지
        if (containsAny(lower, "까칠", "화", "무뚝뚝", "엄격", "진지", "grumpy", "angry", "strict", "serious", "cold", "tough")) {
            return "onyx";
        }

        // 밝음/활발/재미/유쾌/에너지
        if (containsAny(lower, "밝", "활발", "재미", "유쾌", "에너지", "cheerful", "energetic", "fun", "lively", "excited", "happy")) {
            return "fable";
        }

        // 친절/따뜻/다정/상냥
        if (containsAny(lower, "친절", "따뜻", "다정", "상냥", "kind", "warm", "gentle", "friendly", "caring", "sweet")) {
            return "nova";
        }

        // 조용/수줍/섬세/부드러운
        if (containsAny(lower, "조용", "수줍", "섬세", "부드", "shy", "quiet", "soft", "delicate", "timid", "calm")) {
            return "shimmer";
        }

        // 무거운/어두운/슬픈
        if (containsAny(lower, "무거", "어두", "슬픈", "우울", "dark", "sad", "gloomy", "melancholy", "deep")) {
            return "echo";
        }

        return "alloy";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}