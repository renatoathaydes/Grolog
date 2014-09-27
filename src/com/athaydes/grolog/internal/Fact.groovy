package com.athaydes.grolog.internal

import com.athaydes.grolog.ConditionGrolog
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.ToString

import static java.util.Collections.emptySet

@ToString( includePackage = false, includeFields = true )
@EqualsAndHashCode
class Fact {

    final String name
    final Object[] args
    final Condition condition

    Fact( String name, Object[] args, Condition condition = new Condition( emptySet() ) ) {
        this.name = name
        this.args = args
        this.condition = condition
    }

}

class UnboundedFact extends Fact {

    UnboundedFact( String name, Object[] args, Set<UnboundedVar> unboundedVars ) {
        super( name, args, new Condition( unboundedVars ) )
    }

}

class Condition {

    private final ConditionGrolog grolog

    Condition( Set<UnboundedVar> unboundedVars ) {
        this.grolog = new ConditionGrolog( unboundedVars )
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