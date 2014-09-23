package com.athaydes.grolog

import com.athaydes.grolog.internal.parser.GrologParser
import org.apache.tools.ant.filters.StringInputStream

import java.util.regex.Matcher

class GrologRunner {

    static final USAGE = """|************ Grolog ************
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
                            def parts = parser.parseQuery( input )
                            println( ( isQuery( parts ) ) ?
                                    grolog.query( parts[ 0 ], parts[ 1 ] ) :
                                    "Enter '!help' for usage." )
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

    static boolean isQuery( queryResult ) {
        queryResult && queryResult instanceof List && queryResult.size() == 2 &&
                queryResult[ 0 ] instanceof String && queryResult[ 1 ] instanceof Object[]
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
