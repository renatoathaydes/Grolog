package com.athaydes.grolog

import com.athaydes.grolog.internal.*

class Grolog {

    protected final Inserter inserter
    protected final Querier querier

    protected Grolog( Set<UnboundedVar> unboundedVars ) {
        def facts = [ : ]
        this.inserter = new Inserter( facts, unboundedVars )
        this.querier = createQuerier( facts )
    }

    public Grolog() {
        this( [ ] as Set<UnboundedVar> )
    }

    protected createQuerier( Map facts ) {
        new Querier( facts )
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

    QueryResult query( String q, Object... args ) {
        println "Query $q ? $args --- Facts: ${querier.allFacts()}"
        def result = querier.query q, args
        switch ( result ) {
            case Boolean: return QueryResult.TrueFalse( result )
            case Iterable: return QueryResult.MultipleBindings( result )
            case String: return QueryResult.Error( result )
        }
    }

    def with( Closure config ) {
        def result = super.with config
        verify()
        result
    }

    protected void verify() {
        for ( fact in querier.allFacts() ) {
            if ( fact instanceof UnboundedFact && !fact.condition.hasClauses() ) {
                throw new InvalidDeclaration( "Invalid Fact (unbounded without clause): $fact" )
            }
        }
    }

}

class ConditionGrolog extends Grolog {

    ConditionGrolog( Set<UnboundedVar> unboundedVars ) {
        super( unboundedVars )
    }

    @Override
    protected createQuerier( Map facts ) {
        new ConditionQuerier( facts )
    }

    @Override
    protected void verify() {
        // no verification for conditions!
    }

}