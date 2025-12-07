package com.yourorg.aiplatform.agentapi.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        // SimpleVectorStore keeps embeddings in-memory, making it easy to swap for pgvector or Cosmos later.
        return new SimpleVectorStore(embeddingClient);
    }
}
