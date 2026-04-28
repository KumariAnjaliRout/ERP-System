package com.app.EMS.exception;

public class  MethodArgumentNotValidException extends RuntimeException{
    public MethodArgumentNotValidException(String msg){
        super(msg);
    }
}

