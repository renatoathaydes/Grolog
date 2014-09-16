package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.Condition

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private Map<String, List<Fact>> facts = [ : ]
    private List<String> propositions = [ ]

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        Fact fact = null
        if ( args ) {
            facts.get( name, [ ] ) << ( fact = new Fact( this, name, args ?: [ ] ) )
        } else {
            propositions << name
        }
        new Condition( fact )
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

    private static void match( List<Fact> facts, Object[] args, int index,
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

    private static List<Fact> factMatches( List<Fact> facts, Object[] args, int index ) {
        facts.findAll { it.args[ index ] == args[ index ] }
    }

    private static void bindMatches( List maybeMatches, List<Fact> trueFacts ) {
        if ( !trueFacts ) {
            return
        }
        for ( maybeMatch in maybeMatches ) {
            AtomicReference current = maybeMatch[ 0 ]
            List<Fact> maybeFacts = maybeMatch[ 1 ]
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
