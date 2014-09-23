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
        def grolog = new GrologParser().parsePredicates( input )
        assert grolog.query( 'water' )
        assert grolog.query( 'person', 'Renato' )
        assert grolog.query( 'worker', 'Renato' )
        assert grolog.query( 'whatever' ) == false
    }

    void testGrologQuery() {
        def parser = new GrologParser()
        parser.parseQuery( 'hello' ) == [ 'hello', [ ] ]
        parser.parseQuery( 'hello();' ) == [ 'hello', [ ] ]
        parser.parseQuery( 'hello "world"' ) == [ 'hello', [ 'world' ] ]
        parser.parseQuery( 'hello 1, 2, 3' ) == ['hello', [1, 2, 3]]
        parser.parseQuery( '' ) == ''
        parser.parseQuery( '10' ) == 10
    }

}
