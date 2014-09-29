package com.athaydes.grolog.internal.parser

import com.athaydes.grolog.Var
import org.apache.tools.ant.filters.StringInputStream

import java.util.concurrent.atomic.AtomicReference

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
        assert grolog.query( 'water' ).exists
        assert grolog.query( 'person', 'Renato' ).exists
        assert grolog.query( 'worker', 'Renato', Var._ ).exists
        assert !grolog.query( 'whatever' ).exists
    }

    void testGrologQuery() {
        def parser = new GrologParser()
        assert parser.parseQuery( 'hello' ) == [ 'hello', [ ] ]
        assert parser.parseQuery( 'hello();' ) == [ 'hello', [ ] ]
        assert parser.parseQuery( 'hello "world"' ) == [ 'hello', [ 'world' ] ]
        assert parser.parseQuery( 'hello 1, 2, 3' ) == [ 'hello', [ 1, 2, 3 ] ]
        use(CompareAtomicReferenceByValue) {
            assert parser.parseQuery( 'hello hi' ) == [ 'hello', [ new AtomicReference( 'hi' ) ] ]
        }

        // bad input
        assert parser.parseQuery( '' ) == ''
        assert parser.parseQuery( '10' ) == 10
    }

    @Category( AtomicReference )
    class CompareAtomicReferenceByValue {
        boolean equals( other ) {
            other instanceof AtomicReference && other.get() == this.get()
        }
    }

}
