package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference

class GrologTest extends GroovyTestCase {

    void testSimpleNoArgsFacts() {
        def grolog = new Grolog()
        grolog.with {
            man 'John'
            woman 'Mary'
            man 'Mike'
        }

        assert grolog.query( 'man' ) == [ 'John', 'Mike' ]
        assert grolog.query( 'woman' ) == [ 'Mary' ]
    }

    void testSimple1ArgFacts() {
        def grolog = new Grolog()
        grolog.with {
            man 'John'
            woman 'Mary'
            man 'Mike'
        }

        assert grolog.query( 'man', 'John' ) == true
        assert grolog.query( 'woman', 'John' ) == false
        assert grolog.query( 'woman', 'Mary' ) == true
        assert grolog.query( 'man', 'Mike' ) == true
        assert grolog.query( 'man', 'Mary' ) == false
    }

    void testSimple2ArgsFactsResolving2ndArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def johnsFather = new AtomicReference( null )
        grolog.query( 'father', 'John', johnsFather )
        assert johnsFather.get() == 'Paul'

        def marysFather = new AtomicReference( null )
        grolog.query( 'father', 'Mary', marysFather )
        assert marysFather.get() == 'John'
    }

    void testSimple2ArgsFactsResolvingFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def paulsSon = new AtomicReference( null )
        grolog.query( 'father', paulsSon, 'Paul' )
        assert paulsSon.get() == 'John'

        def childOfJohn = new AtomicReference( null )
        grolog.query( 'father', childOfJohn, 'John' )
        assert childOfJohn.get() == 'Mary'
    }


    void testSimple2ArgsFactsNoMatchFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsSon = new AtomicReference( null )
        grolog.query( 'father', richardsSon, 'Richard' )
        assert richardsSon.get() == null
    }

    void testSimple2ArgsFactsNoMatchSecondArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsFather = new AtomicReference( null )
        grolog.query( 'father', 'Richard', richardsFather )
        assert richardsFather.get() == null
    }

    void testSimple2ArgsFactsBothUnboundVariables() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def child = new AtomicReference( null )
        def father = new AtomicReference( null )
        grolog.query( 'father', child, father )
        assert child.get() == [ 'John', 'Mary' ]
        assert father.get() == [ 'Paul', 'John' ]
    }

}
