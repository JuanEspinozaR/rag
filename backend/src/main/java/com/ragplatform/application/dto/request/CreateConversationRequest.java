package com.ragplatform.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
}
