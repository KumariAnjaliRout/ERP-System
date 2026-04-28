package com.app.EMS.exception;

public class AlreadyExistsResourceException extends RuntimeException{
    public AlreadyExistsResourceException(String msg){
        super(msg);
    }
}
