// src/main/java/com/example/demo/dto/NewsletterDTO.java
package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsletterDTO {
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private String email;

    public NewsletterDTO() {}

    public NewsletterDTO(String email) {
        this.email = email;
    }
}
