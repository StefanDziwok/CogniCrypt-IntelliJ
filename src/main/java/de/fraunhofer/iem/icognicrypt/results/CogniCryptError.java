package de.fraunhofer.iem.icognicrypt.results;

import crypto.analysis.errors.AbstractError;

public class CogniCryptError {

    private String errorMessage;
    private String className;

    public CogniCryptError(String error, String className) {
        this.errorMessage = error;
        this.className = className;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getClassName(){
        return className;
    }

}
