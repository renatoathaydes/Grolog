package com.athaydes.grolog.internal

import com.athaydes.grolog.ConditionGrolog
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.ToString

@ToString( includePackage = false, includeFields = true )
@EqualsAndHashCode
class Fact {

    final String name
    final Object[] args
    final Condition condition

    Fact( String name, Object[] args ) {
        this.name = name
        this.args = args
        this.condition = new Condition()
    }

}

class UnboundedFact extends Fact {

    UnboundedFact( String name, Object[] args ) {
        super( name, args )
    }

}

class Condition {

    private final ConditionGrolog grolog

    Condition() {
        this.grolog = new ConditionGrolog()
    }

    void iff( Closure clausesCallback ) {
        grolog.with clausesCallback
    }

    boolean hasClauses() {
        grolog.allFacts()
    }

    Set<Fact> allClauses() {
        grolog.allFacts()
    }

    @Override
    boolean equals( other ) {
        other instanceof Condition && this.allClauses() == other.allClauses()
    }

    @Override
    int hashCode() {
        allClauses().hashCode()
    }

    @Override
    String toString() {
        def statements = grolog.allFacts()
        if ( statements ) {
            "Cond($statements)"
        } else {
            '_'
        }
    }

}

@Immutable
@ToString( includePackage = false )
class UnboundedVar {
    String name
}