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

    void testSimpleNoArgsFactsClashWithPropositions() {
        def grolog = new Grolog()

        shouldFail( ArityException.class ) {
            grolog.with {
                man;
                man 'John'
            }
        }

        shouldFail( ArityException.class ) {
            grolog.with {
                woman 'Mary';
                woman;
            }
        }

        shouldFail( ArityException.class ) {
            grolog.with {
                car 'Ford', 'Fiesta', 2014;
                car 'BMW', 'M6';
            }
        }
    }

    void testQueryWithWrongNumberOfArgsProducesError() {
        def grolog = new Grolog()
        grolog.with {
            water;
            man 'John'
            married 'John', 'Mary'
        }
        shouldFail( ArityException.class ) {
            grolog.query( 'water', 1 )
        }
        shouldFail( ArityException.class ) {
            grolog.query( 'man', 'John', 1 )
        }
        shouldFail( ArityException.class ) {
            grolog.query( 'married', 'John' )
        }
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

        assert grolog.query( 'father', 'John', johnsFather )
        assert johnsFather.get() == 'Paul'

        def marysFather = new Var( null )

        assert grolog.query( 'father', 'Mary', marysFather )
        assert marysFather.get() == 'John'
    }

    void testSimple2ArgsFactsResolvingFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def paulsSon = new Var( null )

        assert grolog.query( 'father', paulsSon, 'Paul' )
        assert paulsSon.get() == 'John'

        def childOfJohn = new Var( null )

        assert grolog.query( 'father', childOfJohn, 'John' )
        assert childOfJohn.get() == 'Mary'
    }


    void testSimple2ArgsFactsNoMatchFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsSon = new Var( null )

        assert grolog.query( 'father', richardsSon, 'Richard' ) == false
        assert richardsSon.get() == null
    }

    void testSimple2ArgsFactsNoMatchSecondArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsFather = new Var( null )

        assert grolog.query( 'father', 'Richard', richardsFather ) == false
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

        assert grolog.query( 'father', child, father ) == true
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
            happy( 'John' ).iff { parent( 'John' ); happiness; sun; moon; }
            happy( 'Rob' ).iff { parent( 'John' ); happiness; sun; sadness; }
            happy( 'Mary' ).iff { happy( 'Mike' ); sadness }
        }

        assert grolog.query( 'happy', 'John' ) == true
        assert grolog.query( 'happy', 'Rob' ) == false
        assert grolog.query( 'happy', 'Mary' ) == false
    }

    void testGrooviness() {
        def grolog = new Grolog()
        grolog.with {
            [ 'Mercury', 'Venus', 'Earth', 'Mars', 'Saturn', 'Jupiter', 'Uranus', 'Neptune' ].each {
                planet it
            }
        }

        assert grolog.query( 'planet', 'Mercury' ) == true
        assert grolog.query( 'planet', 'Earth' ) == true
        assert grolog.query( 'planet', 'Saturn' ) == true
        assert grolog.query( 'planet', 'Jupiter' ) == true
        assert grolog.query( 'planet', 'Neptune' ) == true

        assert grolog.query( 'planet', 'Sun' ) == false
        assert grolog.query( 'planet', 'Moon' ) == false
    }

    void test1ArgUnboundVariableRules() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato'
            married 'John'
            happy( A ).iff { married( A ) }
        }

        assert grolog.query( 'married', 'Renato' ) == true
        assert grolog.query( 'married', 'John' ) == true
        assert grolog.query( 'happy', 'Renato' ) == true
        assert grolog.query( 'happy', 'John' ) == true

        assert grolog.query( 'married', 'Mary' ) == false
        assert grolog.query( 'happy', 'Mary' ) == false
    }

    void test2ArgsUnboundVariableRules() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        assert grolog.query( 'married', 'Renato', 'Nat' ) == true
        assert grolog.query( 'married', 'Nat', 'Renato' ) == true
        assert grolog.query( 'married', 'John', 'Mary' ) == true
        assert grolog.query( 'married', 'Mary', 'John' ) == true

        assert grolog.query( 'married', 'Mary', 'Renato' ) == false
        assert grolog.query( 'married', 'Renato', 'Mary' ) == false
        assert grolog.query( 'married', 'Renato', 'John' ) == false
    }

    void test2ArgsUnboundVariableRulesResolvingFirstArg() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person = new Var( null )

        assert grolog.query( 'married', person, 'Nat' ) == true
        assert person.get() == 'Renato'
        person = new Var( null )
        assert grolog.query( 'married', person, 'Renato' ) == true
        assert person.get() == 'Nat'

        person = new Var( null )
        assert grolog.query( 'married', person, 'Mary' ) == true
        assert person.get() == 'John'
        person = new Var( null )
        assert grolog.query( 'married', person, 'John' ) == true
        assert person.get() == 'Mary'
    }


    void test2ArgsUnboundVariableRulesResolvingSecondArg() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person = new Var( null )

        assert grolog.query( 'married', 'Nat', person ) == true
        assert person.get() == 'Renato'
        person = new Var( null )
        assert grolog.query( 'married', 'Renato', person ) == true
        assert person.get() == 'Nat'

        person = new Var( null )
        assert grolog.query( 'married', 'Mary', person ) == true
        assert person.get() == 'John'
        person = new Var( null )
        assert grolog.query( 'married', 'John', person ) == true
        assert person.get() == 'Mary'
    }


    void test2ArgsUnboundVariableRulesResolvingBothArgs() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person1 = new Var( null )
        def person2 = new Var( null )

        assert grolog.query( 'married', person1, person2 ) == true
        assert person1.get() == [ 'Mary', 'Nat' ]
        assert person2.get() == [ 'John', 'Renato' ]
    }

    void test3ArgsWithUnboundVariableRulesResolvingFirstArg() {
        def grolog = new Grolog()

        grolog.with {
            author 'The story', 'John', 1994
            author 'Book 10', 'Mary', 2014
        }

        def book = new Var( null )
        assert grolog.query( 'author', book, 'John', new Var( null ) ) == true
        assert book.get() == 'The story'
        book = new Var( null )
        assert grolog.query( 'author', book, 'Mary', 2014 ) == true
        assert book.get() == 'Book 10'

        book = new Var( null )
        assert grolog.query( 'author', book, 'John', 1988 ) == false // wrong year
        assert book.get() == null
    }

}
