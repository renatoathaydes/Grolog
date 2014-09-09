package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference

class Grolog {

    private List facts = [ ]

    def methodMissing( String name, args ) {
        facts << [ ( name ): args ]
        println facts
    }

    void query( String q, Object... args ) {
        def facts = facts.findAll { it."$q" != null }.collect { it.values().flatten() }
        println "Found facts $facts: $q : $args"
        for ( fact in facts ) {
            match fact, args
        }

    }

    private void match( List fact, Object[] args ) {
        if ( !fact || !args ) return
        println "Checking match $fact : $args"
        if ( args.first() instanceof AtomicReference ) {
            args.first().set( fact.first() )
        } else if ( fact.first() == args.first() ) {
            match fact.tail(), args.tail()
        }
    }

}
