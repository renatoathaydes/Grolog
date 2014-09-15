package com.athaydes.grolog

import com.athaydes.grolog.internal.Clause
import com.athaydes.grolog.internal.Condition

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private Map<String, List<Clause>> facts = [ : ]
    private List<String> propositions = [ ]

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        boolean fact = args;
        Clause clause = null
        if ( fact ) {
            facts.get( name, [ ] ) << ( clause = new Clause( this, name, args ?: [ ] ) )
        } else {
            propositions << name
        }
        new Condition( clause )
    }

    def propertyMissing( String name ) {
        println "Missing prop: $name"
        propositions << name
    }

    def query( String q, Object... args ) {
        assert q, 'A query must be provided'
        println "Query $q ? $args --- Facts: $facts"

        def foundFacts = facts[ q ]

        if ( !args ) {
            return propositions.contains( q ) ?:
                    ( foundFacts ? foundFacts*.args.flatten() : false )
        }

        if ( args.size() == 1 ) {
            return foundFacts != null && args[ 0 ] in foundFacts*.args.flatten()
        }

        match foundFacts, args, 0, [ ]
        return null
    }

    private static void match( List<Clause> facts, Object[] args, int index,
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

    private static List<Clause> factMatches( List<Clause> facts, Object[] args, int index ) {
        facts.findAll { it.args[ index ] == args[ index ] }
    }

    private static void bindMatches( List maybeMatches, List<Clause> trueFacts ) {
        if ( !trueFacts ) {
            return
        }
        for ( maybeMatch in maybeMatches ) {
            AtomicReference current = maybeMatch[ 0 ]
            List<Clause> maybeFacts = maybeMatch[ 1 ]
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

    private static void ensureListsWithOneItemAreLifted( List maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            AtomicReference current = maybeMatch[ 0 ]
            if ( current.get() instanceof List && current.get().size() == 1 ) {
                current.set( current.get()[ 0 ] )
            }
        }
    }

}
