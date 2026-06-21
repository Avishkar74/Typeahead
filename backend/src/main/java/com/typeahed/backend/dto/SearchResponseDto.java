package com.typeahed.backend.dto;

public class SearchResponseDto {

    private String message;

    public SearchResponseDto() {
    }

    public SearchResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
