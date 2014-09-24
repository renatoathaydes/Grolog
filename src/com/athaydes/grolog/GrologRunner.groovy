package com.athaydes.grolog

import com.athaydes.grolog.internal.parser.GrologParser
import org.apache.tools.ant.filters.StringInputStream

import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Matcher

class GrologRunner {

    static final USAGE = """|******************** Grolog ********************
                            |Type '!' to add predicates
                            |     '?' to enter queries
                            |
                            |Commands:
                            |  * '!q' or '!quit'     = quit Grolog
                            |  * '!h' or '!help'     = print this usage help
                            |  * '!c' or '!clear'    = clear all predicates
                            |""".stripMargin()

    static void main( String[] args ) {
        println USAGE

        def grolog = new Grolog()
        def parser = new GrologParser()
        def queryMode = false
        print( queryMode ? '?- ' : '!- ' )
        System.in.eachLine { input ->
            try {
                switch ( input ) {
                    case '?': queryMode = true
                        break
                    case ~/!(.*)/:
                        def cmd = matcher[ 0 ][ 1 ]
                        if ( cmd ) {
                            runCmd cmd, grolog
                        } else {
                            queryMode = false
                        }
                        break
                    default:
                        if ( queryMode ) {
                            respondToQuery grolog, parser.parseQuery( input )
                        } else {
                            grolog.merge( parser.parsePredicates( new StringInputStream( input ) ) )
                        }

                }
            } catch ( e ) {
                println "Error: $e"
            }
            print( queryMode ? '?- ' : '!- ' )
        }

    }

    static String queryType( parts ) {
        if ( parts && parts instanceof List && parts.size() == 2 &&
                parts[ 0 ] instanceof String && parts[ 1 ] instanceof Object[] ) {
            return parts[ 1 ].any { it instanceof AtomicReference } ? 'var' : 'truth'
        } else {
            return 'none'
        }
    }

    static void respondToQuery( Grolog grolog, parts ) {
        def respondToTruthQuery = {
            def result = grolog.query( parts[ 0 ], parts[ 1 ] )
            println result
            result
        }
        def respondToVarQuery = {
            def vars = parts[ 1 ].findAll { it instanceof AtomicReference }
            def varNames = vars.collect { it.get() } as LinkedList
            def truth = respondToTruthQuery()
            if ( truth ) {
                vars.each { AtomicReference var ->
                    println "${varNames.pop()} = $var"
                }
            }
        }

        switch ( queryType( parts ) ) {
            case 'truth':
                respondToTruthQuery()
                break
            case 'var':
                respondToVarQuery()
                break
            default:
                println "Enter '!help' for usage."
        }
    }

    static void runCmd( String cmd, Grolog grolog ) {
        switch ( cmd ) {
            case 'q':
            case 'quit':
                println "Bye!"
                System.exit( 0 )
                break // avoid warnings
            case 'h':
            case 'help':
                println USAGE
                break
            case 'c':
            case 'clear':
                grolog.clear()

        }
    }

    static Matcher getMatcher() {
        Matcher.lastMatcher
    }

}
