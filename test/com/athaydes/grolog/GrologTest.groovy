package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference

class GrologTest extends GroovyTestCase {

    void testSimple2ArgsFactsResolving2ndArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def johnsFather = new AtomicReference( false )
        grolog.query( 'father', 'John', johnsFather )
        assert johnsFather.get() == 'Paul'

        def marysFather = new AtomicReference( false )
        grolog.query( 'father', 'Mary', marysFather )
        assert marysFather.get() == 'John'
    }

    void testSimple2ArgsFactsResolvingFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def paulsSon = new AtomicReference( false )
        grolog.query( 'father', paulsSon, 'Paul' )
        assert paulsSon.get() == 'John'

        def childOfJohn = new AtomicReference( false )
        grolog.query( 'father', childOfJohn, 'John' )
        assert childOfJohn.get() == 'Mary'
    }

}
