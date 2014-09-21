package com.athaydes.grolog.internal.parser

import com.athaydes.grolog.Grolog
import org.apache.tools.ant.filters.StringInputStream
import org.codehaus.groovy.control.CompilerConfiguration

class GrologParser {

    Grolog from( InputStream input, String charset = 'utf8' ) {
        def config = new CompilerConfiguration()
        config.scriptBaseClass = GrologScriptBase.class.name

        def engine = new GroovyShell( config )
        engine.evaluate( new SequenceInputStream(
                input, new StringInputStream('\n_grolog') ).newReader( charset ) )
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
