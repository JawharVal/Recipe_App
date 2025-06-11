package com.example.demo.dto;


public class GenerationLimitResponse {
    private boolean allowed;
    private int remaining;
    private int currentCount;
    private int limit;

    public GenerationLimitResponse(boolean allowed, int remaining, int currentCount, int limit) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.currentCount = currentCount;
        this.limit = limit;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
