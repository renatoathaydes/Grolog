package com.athaydes.grolog.internal

import com.athaydes.grolog.Grolog
import groovy.transform.ToString

@ToString( includePackage = false, includeFields = true, includes = [ 'name', 'args' ] )
class Clause {

    final Grolog grolog
    final String name
    final Object[] args
    Condition condition

    Clause( Grolog grolog, String name, Object[] args ) {
        this.grolog = grolog
        this.name = name
        this.args = args
    }

}

class Condition {

    private final Grolog grolog
    final Clause fact

    Condition( Clause fact ) {
        this.fact = fact
        this.grolog = fact ? new Grolog() : null
    }

    void iff( Closure clausesCallback ) {
        grolog?.with clausesCallback
    }

}
