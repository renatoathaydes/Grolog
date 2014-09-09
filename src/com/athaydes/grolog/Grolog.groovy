package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private List facts = [ ]

    def methodMissing( String name, args ) {
        facts << [ ( name ): args ]
    }

    def query( String q, Object... args ) {
        assert q, 'A query must be provided'

        def foundFacts = facts.findAll { it."$q" != null }.collect { it.values().flatten() }

        if ( !args ) {
            return foundFacts.flatten()
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
