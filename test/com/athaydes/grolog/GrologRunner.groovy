package com.athaydes.grolog

import com.athaydes.grolog.internal.parser.GrologParser
import org.apache.tools.ant.filters.StringInputStream

class GrologRunner {

    static void main( String[] args ) {
        def grolog = new Grolog()
        def grologParser = new GrologParser()
        def queryMode = false
        print( queryMode ? '?- ' : '!- ' )
        System.in.eachLine { input ->
            try {
                switch ( input ) {
                    case '?': queryMode = true
                        break
                    case '!': queryMode = false
                        break
                    case 'q':
                        println "Bye!"
                        System.exit( 0 )
                    default:
                        if ( queryMode ) {
                            def parts = input.split()
                            if ( !parts ) println 'You must provide a command'
                            else println grolog.query( parts.first(), *parts.tail() )
                        } else {
                            grolog.merge( grologParser.from( new StringInputStream( input ) ) )
                        }

                }
            } catch ( e ) {
                println "Error: $e"
            }
            print( queryMode ? '?- ' : '!- ' )
        }

    }

}
