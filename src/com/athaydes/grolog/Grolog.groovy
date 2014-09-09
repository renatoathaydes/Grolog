package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private List facts = [ ]
    private List propositions = [ ]

    def methodMissing( String name, args ) {
        println "Missing method $name, $args"
        def factsSize = facts.size()
        def propositionsSize = propositions.size()
        if ( args ) {
            facts << [ ( name ): args ]
        } else {
            propositions << name
        }
        return [ iff: { previousMap ->
            def prev = previousMap.prev()
            println "IFF $prev"
            def ruleOk = ( prev instanceof Map ) ?
                    internalQuery( prev.keySet().first(), facts[ 0..<factsSize ], [ ], prev.values().first() ) :
                    internalQuery( prev, [ ], propositions[ 0..<propositionsSize ] )
            println "Rule ok? $ruleOk"
            if ( !ruleOk ) {
                if ( args ) {
                    println "Removing ${facts[factsSize-1]}"
                    facts.remove( factsSize - 1 )
                } else {
                    propositions.remove( propositionsSize - 1 )
                }
            }
            if (prev instanceof Map) {
                facts.remove( prev )
            } else {
                propositions.remove( prev )
            }
        }, prev     : {
            if ( args ) {
                [ ( name ): args ]
            } else {
                name
            }
        } ]
    }

    def propertyMissing( String name ) {
        println "Missing prop: $name"
        propositions << name
    }

    def query( String q, Object... args ) {
        assert q, 'A query must be provided'
        internalQuery( q, facts, propositions, args )
    }

    private internalQuery( String q, List facts, List propositions, Object... args ) {
        println "Query $q ? $args --- Facts: $facts"
        def foundFacts = facts.findAll { it."$q" != null }.collect { it.values().flatten() }

        if ( !args ) {
            return propositions.contains( q ) ?: foundFacts.flatten()
        }

        if ( args.size() == 1 ) {
            return args[ 0 ] in foundFacts.flatten()
        }

        for ( fact in foundFacts ) {
            match fact, args, 0, [ ]
        }
    }

    private void match( List fact, Object[] args, int index, List maybeMatches ) {
        if ( index >= fact.size() || index >= args.size() ) {
            bindMatches maybeMatches
            return
        }

        def queryArg = args[ index ] instanceof AtomicReference

        if ( queryArg ) {
            maybeMatches << [ args[ index ], fact[ index ] ]
        }
        if ( queryArg || fact[ index ] == args[ index ] ) {
            match fact, args, index + 1, maybeMatches
        }
    }

    private void bindMatches( List maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            def current = maybeMatch[ 0 ].get()
            if ( current == null ) {
                maybeMatch[ 0 ].set maybeMatch[ 1 ]
            } else if ( current instanceof List ) {
                current << maybeMatch[ 1 ]
            } else {
                maybeMatch[ 0 ].set( [ current, maybeMatch[ 1 ] ] )
            }
        }
    }

}
