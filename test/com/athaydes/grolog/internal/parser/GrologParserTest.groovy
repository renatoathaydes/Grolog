package com.athaydes.grolog.internal.parser

import org.apache.tools.ant.filters.StringInputStream

/**
 *
 */
class GrologParserTest extends GroovyTestCase {

    void testGrologParser() {
        def input = new StringInputStream( """
            water
            person 'Renato'
            worker 'Renato', 'Engineer'
            """ )
        def grolog = new GrologParser().from( input )
        assert grolog.query( 'water' )
        assert grolog.query( 'person', 'Renato' )
        assert grolog.query( 'worker', 'Renato' )
        assert grolog.query( 'whatever' ) == false
    }

}
