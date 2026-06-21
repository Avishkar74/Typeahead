package com.typeahed.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

    private String error;
    private String message;
    private List<String> details;

    public ErrorResponseDto() {
    }

    public ErrorResponseDto(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public ErrorResponseDto(String error, String message, List<String> details) {
        this.error = error;
        this.message = message;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
