package com.yourorg.aiplatform.agentapi.config;

import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.ai.chat.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public ChatClient chatClient(AzureOpenAiChatClient azureOpenAiChatClient) {
        return azureOpenAiChatClient;
    }
}
