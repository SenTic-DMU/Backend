package com.project.sentic.infra.ai;

import com.project.sentic.infra.ai.dto.ChatCompletionResponse;
import com.project.sentic.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final OpenAiClient openAiClient;

    public String chat(String userMessage) {
        List<ChatMessage> messages = List.of(ChatMessage.user(userMessage));
        ChatCompletionResponse response = openAiClient.chat(messages);
        return response.getContent();
    }

    public String chatWithSystem(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = List.of(
                ChatMessage.system(systemPrompt),
                ChatMessage.user(userMessage)
        );
        ChatCompletionResponse response = openAiClient.chat(messages);
        return response.getContent();
    }

    public ChatCompletionResponse chatWithHistory(String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.addAll(history);
        messages.add(ChatMessage.user(userMessage));
        return openAiClient.chat(messages);
    }
}
