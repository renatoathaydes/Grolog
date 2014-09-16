package com.athaydes.grolog.internal

import com.athaydes.grolog.Grolog
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString( includePackage = false, includeFields = true,
        includes = [ 'name', 'args', 'condition' ] )
@EqualsAndHashCode( includes = [ 'name', 'args' ] )
class Fact {

    final String name
    final Object[] args
    final Condition condition = new Condition()

    Fact( String name, args ) {
        this.name = name
        this.args = args
    }

}

class Condition {

    private final Grolog grolog = new Grolog()

    void iff( Closure clausesCallback ) {
        grolog.with clausesCallback
    }

    boolean satisfiedBy( Grolog other ) {
        this.grolog.trueFacts().every { Fact fact ->
            other.queryInternal( false, fact.name, fact.args )
        }
    }

    @Override
    String toString() {
        def statements = grolog.trueFacts()
        if ( statements ) {
            "Cond($statements)"
        } else {
            '_'
        }
    }

}
