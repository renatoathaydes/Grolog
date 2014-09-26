package com.athaydes.grolog

class ArityException extends RuntimeException {

    ArityException( String fact, int arity ) {
        super( "Fact '$fact' has arity $arity" )
    }

}

class InvalidDeclaration extends RuntimeException {

    InvalidDeclaration(String message) {
        super(message)
    }

}