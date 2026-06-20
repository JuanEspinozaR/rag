package com.ragplatform.infrastructure.langfuse;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "langfuse")
public class LangfuseProperties {
    private boolean enabled = true;
    private String publicKey;
    private String secretKey;
    private String host = "http://localhost:3001";
}
