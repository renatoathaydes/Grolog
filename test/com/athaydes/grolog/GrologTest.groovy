package com.athaydes.grolog

import java.util.concurrent.atomic.AtomicReference as Var

class GrologTest extends GroovyTestCase {

    void testPropositions() {
        def grolog = new Grolog()
        grolog.with {
            world;
            water()
        }

        assert grolog.query( 'world' ) == true
        assert grolog.query( 'water' ) == true
        assert grolog.query( 'fire' ) == false
    }

    void testSimpleNoArgsFacts() {
        def grolog = new Grolog()
        grolog.with {
            man 'John'
            woman 'Mary'
            man 'Mike'
        }

        assert grolog.query( 'man' ) as Set == [ 'John', 'Mike' ] as Set
        assert grolog.query( 'woman' ) as Set == [ 'Mary' ] as Set

        assert grolog.query( 'undecided' ) == false
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

        assert grolog.query( 'undecided', 'Mary' ) == false
        assert grolog.query( 'undecided', 'Ivy' ) == false
    }

    void testSimple2ArgsFactsResolving2ndArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def johnsFather = new Var( null )
        grolog.query( 'father', 'John', johnsFather )
        assert johnsFather.get() == 'Paul'

        def marysFather = new Var( null )
        grolog.query( 'father', 'Mary', marysFather )
        assert marysFather.get() == 'John'
    }

    void testSimple2ArgsFactsResolvingFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def paulsSon = new Var( null )
        grolog.query( 'father', paulsSon, 'Paul' )
        assert paulsSon.get() == 'John'

        def childOfJohn = new Var( null )
        grolog.query( 'father', childOfJohn, 'John' )
        assert childOfJohn.get() == 'Mary'
    }


    void testSimple2ArgsFactsNoMatchFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsSon = new Var( null )
        grolog.query( 'father', richardsSon, 'Richard' )
        assert richardsSon.get() == null
    }

    void testSimple2ArgsFactsNoMatchSecondArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsFather = new Var( null )
        grolog.query( 'father', 'Richard', richardsFather )
        assert richardsFather.get() == null
    }

    void testSimple2ArgsFactsBothUnboundVariables() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def child = new Var( null )
        def father = new Var( null )
        grolog.query( 'father', child, father )
        assert child.get() == [ 'John', 'Mary' ]
        assert father.get() == [ 'Paul', 'John' ]
    }

    void testSimpleRules() {
        def grolog = new Grolog()
        grolog.with {
            parent 'John'
            happy( 'John' ).iff { parent( 'John' ) }
            happy( 'Mary' ).iff { happy( 'Mike' ) }
        }

        assert grolog.query( 'happy', 'John' ) == true
        assert grolog.query( 'happy', 'Mary' ) == false
    }

    void testRulesWithManyPredicates() {
        def grolog = new Grolog()
        grolog.with {
            happiness;
            sun;
            moon;
            parent 'John'
            happy( 'John' ).iff { parent( 'John' ) ; happiness ; sun ; moon ; }
            happy( 'Rob' ).iff { parent( 'John' ) ; happiness ; sun ; sadness ; }
            happy( 'Mary' ).iff { happy( 'Mike' ) ; sadness }
        }

        assert grolog.query( 'happy', 'John' ) == true
        assert grolog.query( 'happy', 'Rob' ) == false
        assert grolog.query( 'happy', 'Mary' ) == false
    }

}
