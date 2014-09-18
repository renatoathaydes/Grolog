package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.UnboundedVar
import groovy.transform.PackageScope

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private final Map<String, Set<Fact>> facts = [ : ]
    protected final Set<UnboundedVar> unboundedVars

    protected Grolog( Set<UnboundedVar> unboundedVars ) {
        this.unboundedVars = unboundedVars
    }

    public Grolog() {
        this( [ ] as Set<UnboundedVar> )
    }

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        def fact = new Fact( name, args, drain( unboundedVars ) )
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

    def query( String q, Object... args ) {
        assert q, 'A query must be provided'
        println "Query $q ? $args --- Facts: $facts"
        queryInternal( true, q, args )
    }

    @PackageScope
    def queryInternal( boolean checkCondition, String q, args ) {
        def foundFacts = checkCondition ? maybeTrueFacts( q ) : facts[ q ]

        if ( !args || !foundFacts ) {
            if ( foundFacts && foundFacts.every { it.args.size() == 0 } ) {
                return true
            }
            return foundFacts ? foundFacts*.args.flatten() : false
        }

        match foundFacts, args, 0, [ ]
    }

    boolean trueThing( Fact f ) { !f.condition || f.condition.satisfiedBy( this ) }

    boolean nonDeterministicFact( Fact f ) { f.args.any { it instanceof UnboundedVar } }

    boolean maybeTrueThing( Fact f ) { nonDeterministicFact( f ) || trueThing( f ) }

    Set<Fact> maybeTrueFacts( String name = null ) {
        if ( name ) {
            ( facts[ name ]?.findAll( this.&maybeTrueThing ) ) ?: [ ]
        } else {
            facts.values().flatten().findAll this.&maybeTrueThing
        }
    }

    private match( Set<Fact> facts, Object[] args, int index,
                   List maybeMatches ) {
        def candidateFacts = facts.findAll { it.args.size() >= index }
        if ( index >= args.size() ) {
            return ( candidateFacts ? bindMatches( maybeMatches, candidateFacts, args ) : true )
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

    private Set<Fact> factMatches( Set<Fact> facts, Object[] args, int index ) {
        facts.findAll { it.args[ index ] instanceof UnboundedVar || it.args[ index ] == args[ index ] }
    }

    private bindMatches( List<List> maybeMatches, Set<Fact> trueFacts, Object[] args ) {
        if ( !trueFacts ) {
            return false
        }
        if ( trueFacts.any { it.args.any { it instanceof UnboundedVar } } ) {
            return resolveUnboundedArgs( trueFacts, args )
        }
        if ( !maybeMatches ) {
            return true
        }
        for ( maybeMatch in maybeMatches ) {
            maybeMatchQuery( maybeMatch, trueFacts )
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

        if ( current.get() == null ) {
            current.set currentValue
        } else {
            current.get().addAll currentValue
        }
    }

    private resolveUnboundedArgs( Set<Fact> trueFacts, Object[] queryArgs ) {
        println "Unbounded case: $trueFacts"
        trueFacts.every {
            it.condition.unboundedVarResolves( this, queryArgs )
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
        Collections.emptySet()
    }


}