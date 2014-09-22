package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.UnboundedFact
import com.athaydes.grolog.internal.UnboundedVar
import groovy.transform.PackageScope

import java.util.concurrent.atomic.AtomicReference

import static java.util.Collections.emptySet

class Grolog {

    private final Map<String, Set<Fact>> facts = [ : ]
    protected final Set<UnboundedVar> unboundedVars

    protected Grolog( Set<UnboundedVar> unboundedVars ) {
        this.unboundedVars = unboundedVars
    }

    public Grolog() {
        this( [ ] as Set<UnboundedVar> )
    }

    void merge( Grolog other ) {
        other.facts.each { name, fs ->
            facts.get( name, [ ] as Set ).addAll fs
        }
    }

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        def fact = unboundedVars ? new UnboundedFact( name, args, drain( unboundedVars ) ) : new Fact( name, args )
        facts.get( name, [ ] as Set ) << fact
        fact.condition
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

    protected Fact trueThing( Fact fact, Object[] args, boolean resolveUnboundedAndConditionalFacts ) {
        if ( !resolveUnboundedAndConditionalFacts && ( fact instanceof UnboundedFact || fact.condition.hasClauses() ) ) {
            return null
        }
        def conditions = fact.condition.allClauses()
        if ( !conditions ) {
            return ( fact instanceof UnboundedFact ) ? null : fact
        }

        Map<String, Object> boundedArgs = ( fact instanceof UnboundedFact ) ? fact.boundedArgs( args ) : null
        def conditionsSatisfied = conditions.every { Fact cond -> conditionSatisfied( cond, boundedArgs ) }

        if ( conditionsSatisfied ) {
            return boundedArgs ? new Fact( fact.name, resolvedArgs( fact.args, boundedArgs ) ) : fact
        } else {
            return null
        }
    }

    protected boolean conditionSatisfied( Fact cond, Map<String, Object> boundedArgs ) {
        Object[] actualArgs = boundedArgs ? resolvedArgs( cond.args, boundedArgs ) : cond.args
        queryInternal( false, cond.name, actualArgs )
    }

    protected Fact maybeTrueThing( Object[] args, Fact f ) {
        f.args.any { it instanceof AtomicReference } ? f : trueThing( f, args, true )
    }

    private Set<Fact> filterFacts( String name, Closure<Fact> filter ) {
        if ( name ) {
            facts.get( name )?.findResults filter
        } else {
            facts.values().flatten().findResults filter
        }
    }

    Set<Fact> maybeTrueFacts( String name = null, Object[] args = [ ] ) {
        filterFacts( name ) { Fact fact ->
            maybeTrueThing( args, fact )
        }
    }

    Set<Fact> trueFacts( String name = null, Object[] args = [ ] ) {
        filterFacts( name ) { Fact fact ->
            trueThing( fact, args, false )
        }
    }

    Set<Fact> allFacts() {
        facts.values().flatten()
    }

    def query( String q, Object... args ) {
        assert q, 'A query must be provided'
        println "Query $q ? $args --- Facts: $facts"
        queryInternal( true, q, args )
    }

    @PackageScope
    def queryInternal( boolean checkCondition, String q, Object[] args ) {
        def foundFacts = checkCondition ? maybeTrueFacts( q, args ) : trueFacts( q, args )

        if ( !args || !foundFacts ) {
            if ( foundFacts && foundFacts.every { it.args.size() == 0 } ) {
                return true
            }
            return foundFacts ? foundFacts*.args.flatten() : false
        }

        match foundFacts, args, 0, [ ]
    }

    protected Object[] resolvedArgs( Object[] args, Map<String, Object> boundedArgs ) {
        args.collect { arg ->
            if ( arg instanceof UnboundedVar ) {
                boundedArgs[ arg.name ]
            } else {
                arg
            }
        }
    }

    private match( Set<Fact> facts, Object[] args, int index,
                   List maybeMatches ) {
        def candidateFacts = facts.findAll { it.args.size() >= index }
        if ( index >= args.size() ) {
            return ( candidateFacts ? bindMatches( maybeMatches, candidateFacts ) : true )
        }

        def queryArg = args[ index ] instanceof AtomicReference
        def matchingFacts = null

        if ( queryArg ) {
            maybeMatches << [ args[ index ], candidateFacts, index ]
        } else {
            matchingFacts = factMatches( candidateFacts, args, index )
        }

        if ( queryArg || matchingFacts ) {
            match( queryArg ? candidateFacts : matchingFacts, args, index + 1, maybeMatches )
        } else {
            false
        }
    }

    private Set<Fact> factMatches( Set<Fact> facts, Object[] args, int argIndex ) {
        facts.findAll { it.args[ argIndex ] instanceof UnboundedVar || it.args[ argIndex ] == args[ argIndex ] }
    }

    private bindMatches( List<List> maybeMatches, Set<Fact> candidateFacts ) {
        if ( !candidateFacts ) {
            return false
        }

        if ( !maybeMatches ) {
            return true
        }

        for ( maybeMatch in maybeMatches ) {
            maybeMatchQuery( maybeMatch, candidateFacts.findAll { trueThing( it, [ ] as Object[], false ) } )
        }

        ensureListsWithOneItemAreLifted maybeMatches
        maybeMatches.every { maybeMatch ->
            def current = maybeMatch[ 0 ]
            ( current instanceof AtomicReference<List> ? current.get() != null : true )
        }
    }

    private void maybeMatchQuery( List maybeMatch, Set<Fact> trueFacts ) {
        AtomicReference<List> current = maybeMatch[ 0 ]
        Set<Fact> maybeFacts = maybeMatch[ 1 ]
        int index = maybeMatch[ 2 ]

        def currentValue = maybeFacts.findAll { it in trueFacts }.collect { it.args[ index ] }

        if ( currentValue.any { it instanceof AtomicReference } ) {
            return
        }

        if ( current.get() == null ) {
            current.set currentValue
        } else {
            current.get().addAll currentValue
        }
    }

    private void ensureListsWithOneItemAreLifted( List<List> maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            def current = maybeMatch[ 0 ]
            if ( current instanceof AtomicReference<List> && current.get() && current.get().size() == 1 ) {
                current.set( current.get()[ 0 ] )
            }
        }
    }

    protected Set drain( Set set ) {
        def copy = new LinkedHashSet( set )
        set.clear()
        copy
    }

}

class ConditionGrolog extends Grolog {

    ConditionGrolog( Set<UnboundedVar> unboundedVars ) {
        super( unboundedVars.asImmutable() )
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

    protected Set drain( Set set ) {
        emptySet()
    }


}