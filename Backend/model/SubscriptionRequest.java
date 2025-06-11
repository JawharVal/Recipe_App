package com.example.demo.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequest {
    private String subscriptionType;
    private int durationMonths = 1;
}