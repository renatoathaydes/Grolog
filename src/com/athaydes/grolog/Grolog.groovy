package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.UnboundedVar

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private final Map<String, Set<Fact>> facts = [ : ]
    protected final Set<UnboundedVar> unboundedVars

    protected final Inserter inserter
    protected final Querier querier

    protected Grolog( Set<UnboundedVar> unboundedVars ) {
        this.unboundedVars = unboundedVars
        this.inserter = new Inserter( facts, unboundedVars )
        this.querier = new Querier(facts)
    }

    public Grolog() {
        this( [ ] as Set<UnboundedVar> )
    }

    void clear() {
        facts.clear()
        if ( unboundedVars )
            unboundedVars.clear()
    }

    void merge( Grolog other ) {
        other.facts.each { name, fs ->
            inserter.addFact fs
        }
    }

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        inserter.addFact( name, args as Object[] ).condition
    }

    def propertyMissing( String name ) {
        println "Missing prop: $name"
        if ( name.toCharArray()[ 0 ].upperCase ) {
            def var = new UnboundedVar( name )
            unboundedVars << var
            return var
        }
        methodMissing( name, Collections.emptyList() )
    }

    Set<Fact> allFacts() {
        querier.allFacts()
    }

    def query( String q, Object... args ) {
        println "Query $q ? $args --- Facts: $facts"
        querier.query q, args
    }

}

class ConditionGrolog extends Grolog {

    ConditionGrolog( Set<UnboundedVar> unboundedVars ) {
        super( unboundedVars )
    }

    @Override
    def methodMissing( String name, args ) {
        super.methodMissing( name, args )
        null
    }

    @Override
    def propertyMissing( String name ) {
        if ( name.toCharArray()[ 0 ].upperCase ) {
            unboundedVars.find { it.name == name }
        } else {
            super.propertyMissing( name )
        }
    }

}