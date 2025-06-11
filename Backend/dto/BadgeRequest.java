package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

public class BadgeRequest {
    @Getter @Setter
    private String badge;

    @Getter @Setter
    private int count;
}
