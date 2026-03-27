package com.stockmarket.interfaces;

public interface Validator<T> {
    boolean validate(T argument);
}
