package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.UnboundedVar

class Grolog {

    protected final Inserter inserter
    protected final Querier querier

    protected Grolog( Set<UnboundedVar> unboundedVars ) {
        def facts = [ : ]
        this.inserter = new Inserter( facts, unboundedVars )
        this.querier = new Querier( facts )
    }

    public Grolog() {
        this( [ ] as Set<UnboundedVar> )
    }

    void clear() {
        inserter.clear()
    }

    void merge( Grolog other ) {
        inserter.addFact( other.allFacts() )
    }

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        inserter.addFact( name, args as Object[] ).condition
    }

    def propertyMissing( String name ) {
        println "Missing prop: $name"
        inserter.addVar( name )
    }

    Set<Fact> allFacts() {
        querier.allFacts()
    }

    def query( String q, Object... args ) {
        println "Query $q ? $args --- Facts: ${querier.allFacts()}"
        querier.query q, args
    }

}

class ConditionGrolog extends Grolog {

    ConditionGrolog( Set<UnboundedVar> unboundedVars ) {
        super( unboundedVars )
    }

}