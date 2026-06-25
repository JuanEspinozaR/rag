package com.ragplatform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Rag rag = new Rag();
    private Chunking chunking = new Chunking();

    @Getter
    @Setter
    public static class Rag {
        private int topK = 5;
        private double similarityThreshold = 0.7;
        private int maxContextLength = 8000;
    }

    @Getter
    @Setter
    public static class Chunking {
        private int chunkSize = 512;
        private int chunkOverlap = 64;
    }
}
