package com.athaydes.grolog.internal.parser

import com.athaydes.grolog.Grolog
import org.apache.tools.ant.filters.StringInputStream
import org.codehaus.groovy.control.CompilerConfiguration

import java.util.concurrent.atomic.AtomicReference

class GrologParser implements Parser {

    private final GroovyShell predicateShell
    private final GroovyShell queryShell

    GrologParser() {
        def grologPredicatesConfig = new CompilerConfiguration()
        grologPredicatesConfig.scriptBaseClass = GrologScriptBase.class.name
        predicateShell = new GroovyShell( grologPredicatesConfig )

        def grologQueriesConfig = new CompilerConfiguration()
        grologQueriesConfig.scriptBaseClass = GrologQueryScriptBase.class.name
        queryShell = new GroovyShell( grologQueriesConfig )
    }

    Grolog parsePredicates( InputStream input, String charset = 'utf8' ) {
        predicateShell.evaluate( new SequenceInputStream(
                input, new StringInputStream( '\n_grolog' ) ).newReader( charset ) )
    }

    /**
     * @param query
     * @return [predicate , args] if a query was actually entered.
     */
    def parseQuery( String query ) {
        def result = queryShell.evaluate( query ) ?: ''
        if ( result instanceof AtomicReference && result.get() instanceof String ) {
            [ result.get(), [ ] as Object[] ]
        } else {
            result
        }
    }

}

abstract class GrologScriptBase extends Script {

    final _grolog = new Grolog()

    def methodMissing( String name, def args ) {
        _grolog.methodMissing( name, args )
    }

    def propertyMissing( String name ) {
        _grolog.propertyMissing( name )
    }

}

abstract class GrologQueryScriptBase extends Script {

    def methodMissing( String name, def args ) {
        [ name, args ]
    }

    def propertyMissing( String name ) {
        new AtomicReference( name )
    }

}
