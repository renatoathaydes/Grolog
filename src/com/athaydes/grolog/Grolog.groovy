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
        def foundFacts = checkCondition ? trueFacts( q ) : facts[ q ]

        if ( !args || !foundFacts ) {
            if ( foundFacts && foundFacts.every { it.args.size() == 0 } ) {
                return true
            }
            return foundFacts ? foundFacts*.args.flatten() : false
        }

        match foundFacts, args, 0, [ ]
    }

    private final trueThings = { Fact f -> !f.condition || f.condition.satisfiedBy( this ) }

    Set<Fact> trueFacts( String name = null ) {
        if ( name ) {
            ( facts[ name ]?.findAll( trueThings ) ) ?: [ ]
        } else {
            facts.values().flatten().findAll trueThings
        }
    }

    private static match( Set<Fact> facts, Object[] args, int index,
                          List maybeMatches ) {
        def candidateFacts = facts.findAll { it.args.size() >= index }
        if ( index >= args.size() ) {
            return ( candidateFacts ? bindMatches( maybeMatches, facts ) : true )
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

    private static Set<Fact> factMatches( Set<Fact> facts, Object[] args, int index ) {
        facts.findAll { it.args[ index ] == args[ index ] }
    }

    private static bindMatches( List<List> maybeMatches, Set trueFacts ) {
        if ( !trueFacts ) {
            return false
        }
        if ( !maybeMatches ) {
            return true
        }
        for ( maybeMatch in maybeMatches ) {
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
        ensureListsWithOneItemAreLifted maybeMatches
        maybeMatches.every { maybeMatch ->
            AtomicReference<List> current = maybeMatch[ 0 ]
            current.get() != null
        }
    }

    private static void ensureListsWithOneItemAreLifted( List<List> maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            AtomicReference<List> current = maybeMatch[ 0 ]
            if ( current.get() && current.get().size() == 1 ) {
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