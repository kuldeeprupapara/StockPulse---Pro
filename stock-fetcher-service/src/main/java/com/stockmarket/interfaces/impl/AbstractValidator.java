package com.stockmarket.interfaces.impl;

import com.stockmarket.interfaces.Validator;

public abstract class AbstractValidator<T> implements Validator<T> {
    private Validator<T> validator;

    public void setNext(Validator<T> next) {
        this.validator = next;
    }

}
