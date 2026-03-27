package com.stockmarket.dto;

public class APIRespDTO<T> {

    private int status;
    private String message;
    private T data;

    public static <T> APIRespDTO<T> success(T data) {
        APIRespDTO<T> response = new APIRespDTO<>();
        response.setStatus(1);
        response.setData(data);
        return response;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

