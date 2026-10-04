package com.morph.wallet.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableRequestBody(HttpMessageNotReadableException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Request body is required and must contain valid JSON"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException e){
                System.out.println(e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({
        PessimisticLockingFailureException.class,
        DataIntegrityViolationException.class
})
     public ResponseEntity<?> handleLockFailure(Exception e){
        System.out.println(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "too many request"));
    }

    @ExceptionHandler(Exception.class)
     public ResponseEntity<?> handleException(Exception e){
        System.out.println(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "something went wrong"));
    }


}
