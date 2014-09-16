package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import groovy.transform.PackageScope

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private Map<String, List<Fact>> facts = [ : ]

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        def statement = new Fact( name, args )
        facts.get( name, [ ] ) << statement
        statement.condition
    }

    def propertyMissing( String name ) {
        println "Missing prop: $name"
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

        if ( !args ) {
            if ( foundFacts && foundFacts.every { it.args.size() == 0 } ) {
                return true
            }
            return foundFacts ? foundFacts*.args.flatten() : false
        }

        if ( args.size() == 1 ) {
            return args[ 0 ] in foundFacts*.args.flatten()
        }

        match foundFacts, args, 0, [ ]
        return null
    }

    private final trueThings = { !it.condition || it.condition.satisfiedBy( this ) }

    Set<Fact> trueFacts( String name = null ) {
        if ( name ) {
            ( facts[ name ]?.findAll( trueThings ) ) ?: [ ]
        } else {
            facts.values().flatten().findAll trueThings
        }
    }

    private static void match( Set<Fact> facts, Object[] args, int index,
                               List maybeMatches ) {
        def candidateFacts = facts.findAll { it.args.size() >= index }
        if ( index >= args.size() ) {
            if ( candidateFacts ) {
                bindMatches maybeMatches, facts
            }
            return
        }

        def queryArg = args[ index ] instanceof AtomicReference
        def matchingFacts = null

        if ( queryArg ) {
            maybeMatches << [ args[ index ], candidateFacts, index ]
        } else {
            matchingFacts = factMatches( candidateFacts, args, index )
        }

        if ( queryArg || matchingFacts ) {
            match queryArg ? candidateFacts : matchingFacts, args, index + 1, maybeMatches
        }
    }

    private static Set<Fact> factMatches( Set<Fact> facts, Object[] args, int index ) {
        facts.findAll { it.args[ index ] == args[ index ] }
    }

    private static void bindMatches( List<List> maybeMatches, Set trueFacts ) {
        if ( !trueFacts ) {
            return
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
    }

    private static void ensureListsWithOneItemAreLifted( List<List> maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            AtomicReference<List> current = maybeMatch[ 0 ]
            if ( current.get() && current.get().size() == 1 ) {
                current.set( current.get()[ 0 ] )
            }
        }
    }

}
