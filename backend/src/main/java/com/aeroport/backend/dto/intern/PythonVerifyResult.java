package com.aeroport.backend.dto.intern;

public record PythonVerifyResult(
        boolean isMatch,
        Double distance,
        String message
){

}
