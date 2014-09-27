package com.athaydes.grolog

class GrologTest extends GroovyTestCase {

    void testPropositions() {
        def grolog = new Grolog()
        grolog.with {
            world;
            water()
        }

        assert grolog.query( 'world' ).exists
        assert grolog.query( 'water' ).exists
        assert !grolog.query( 'fire' ).exists
    }

    void testSimple1ArgFactsWithUnboundedArg() {
        def grolog = new Grolog()
        grolog.with {
            man 'John'
            man 'Mike'
            woman 'Mary'
        }

        def var = new Var()

        def result = grolog.query( 'man', var )
        assert result.advance()
        assert var.get() == 'John'

        assert result.advance()
        assert var.get() == 'Mike'

        assert !result.advance()

        result = grolog.query( 'woman', var )
        assert result.advance()
        assert var.get() == 'Mary'
    }

    void testSimple1ArgFactsWithConcreteQuery() {
        def grolog = new Grolog()
        grolog.with {
            man 'John'
            man 'Mike'
            woman 'Mary'
        }

        assert grolog.query( 'man', 'John' ).exists
        assert grolog.query( 'woman', 'Mary' ).exists
        assert grolog.query( 'man', 'Mike' ).exists
        assert !grolog.query( 'woman', 'John' ).exists
        assert !grolog.query( 'man', 'Mary' ).exists
    }

    void testSimple2ArgsFactsResolving2ndArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def johnsFather = new Var()

        def result = grolog.query( 'father', 'John', johnsFather )
        assert result.advance()
        assert johnsFather.get() == 'Paul'
        assert !result.advance()

        def marysFather = new Var()

        result = grolog.query( 'father', 'Mary', marysFather )
        assert result.advance()
        assert marysFather.get() == 'John'
        assert !result.advance()
    }

    void testSimple2ArgsFactsResolvingFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def paulsSon = new Var()

        def result = grolog.query( 'father', paulsSon, 'Paul' )
        assert result.advance()
        assert paulsSon.get() == 'John'
        assert !result.advance()

        def childOfJohn = new Var()

        result = grolog.query( 'father', childOfJohn, 'John' )
        assert result.advance()
        assert childOfJohn.get() == 'Mary'
        assert !result.advance()
    }

    void testSimple2ArgsFactsNoMatchFirstArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsSon = new Var()

        def result = grolog.query( 'father', richardsSon, 'Richard' )
        assert !result.advance()
        assert richardsSon.get() == null
    }

    void testSimple2ArgsFactsNoMatchSecondArg() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'John'
        }

        def richardsFather = new Var()

        def result = grolog.query( 'father', 'Richard', richardsFather )
        assert !result.advance()
        assert richardsFather.get() == null
    }

    void testSimple2ArgsFactsBothUnboundVariables() {
        def grolog = new Grolog()
        grolog.with {
            father 'John', 'Paul'
            father 'Mary', 'Mike'
        }

        def child = new Var()
        def father = new Var()

        def result = grolog.query( 'father', child, father )

        assert result.advance()
        assert child.get() == 'John'
        assert father.get() == 'Paul'

        assert result.advance()
        assert child.get() == 'Mary'
        assert father.get() == 'Mike'

        assert !result.advance()
    }

    void testSimpleRules() {
        def grolog = new Grolog()
        grolog.with {
            parent 'John'
            happy( 'John' ).iff { parent( 'John' ) }
            happy( 'Mary' ).iff { happy( 'Mike' ) }
        }

        assert grolog.query( 'happy', 'John' ).exists
        assert !grolog.query( 'happy', 'Mary' ).exists
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

        assert grolog.query( 'happy', 'John' ).exists
        assert !grolog.query( 'happy', 'Rob' ).exists
        assert !grolog.query( 'happy', 'Mary' ).exists
    }

    void testGrooviness() {
        def grolog = new Grolog()
        grolog.with {
            [ 'Mercury', 'Venus', 'Earth', 'Mars', 'Saturn', 'Jupiter', 'Uranus', 'Neptune' ].each {
                planet it
            }
        }

        assert grolog.query( 'planet', 'Mercury' ).exists
        assert grolog.query( 'planet', 'Earth' ).exists
        assert grolog.query( 'planet', 'Saturn' ).exists
        assert grolog.query( 'planet', 'Jupiter' ).exists
        assert grolog.query( 'planet', 'Neptune' ).exists

        assert !grolog.query( 'planet', 'Sun' ).exists
        assert !grolog.query( 'planet', 'Moon' ).exists
    }

    void test1ArgUnboundVariableRules() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato'
            married 'John'
            happy( A ).iff { married( A ) }
        }

        assert grolog.query( 'married', 'Renato' ).exists
        assert grolog.query( 'married', 'John' ).exists
        assert grolog.query( 'happy', 'Renato' ).exists
        assert grolog.query( 'happy', 'John' ).exists

        assert !grolog.query( 'married', 'Mary' ).exists
        assert !grolog.query( 'happy', 'Mary' ).exists
    }

    void test2ArgsUnboundVariableRules() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        assert grolog.query( 'married', 'Renato', 'Nat' ).exists
        assert grolog.query( 'married', 'Nat', 'Renato' ).exists
        assert grolog.query( 'married', 'John', 'Mary' ).exists
        assert grolog.query( 'married', 'Mary', 'John' ).exists

        assert !grolog.query( 'married', 'Mary', 'Renato' ).exists
        assert !grolog.query( 'married', 'Renato', 'Mary' ).exists
        assert !grolog.query( 'married', 'Renato', 'John' ).exists
    }

    void test2ArgsUnboundVariableRulesResolvingFirstArg() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person = new Var()

        def result = grolog.query( 'married', person, 'Nat' )
        assert result.advance()
        assert person.get() == 'Renato'
        assert !result.advance()

        result = grolog.query( 'married', person, 'Renato' )
        assert result.advance()
        assert person.get() == 'Nat'
        assert !result.advance()

        result = grolog.query( 'married', person, 'Mary' )
        assert result.advance()
        assert person.get() == 'John'
        assert !result.advance()

        result = grolog.query( 'married', person, 'John' )
        assert result.advance()
        assert person.get() == 'Mary'
        assert !result.advance()
    }

    void test2ArgsUnboundVariableRulesResolvingSecondArg() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person = new Var()

        def result = grolog.query( 'married', 'Nat', person )
        assert result.advance()
        assert person.get() == 'Renato'
        assert !result.advance()

        result = grolog.query( 'married', 'Renato', person )
        assert result.advance()
        assert person.get() == 'Nat'
        assert !result.advance()

        result = grolog.query( 'married', 'Mary', person )
        assert result.advance()
        assert person.get() == 'John'
        assert !result.advance()

        result = grolog.query( 'married', 'John', person )
        assert result.advance()
        assert person.get() == 'Mary'
        assert !result.advance()

        result = grolog.query( 'married', 'Bob', person )
        assert !result.advance()
    }

    void test2ArgsUnboundVariableRulesResolvingBothArgs() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato', 'Nat'
            married 'John', 'Mary'
            married( A, B ).iff { married( B, A ) }
        }

        def person1 = new Var()
        def person2 = new Var()

        def result = grolog.query( 'married', person1, person2 )
        assert result.advance()
        assert person1.get() == 'Renato'
        assert person2.get() == 'Nat'
        assert result.advance()
        assert person1.get() == 'John'
        assert person2.get() == 'Mary'
        assert !result.advance()
    }

    void test3ArgsWithUnboundVariableRulesResolvingFirstArg() {
        def grolog = new Grolog()

        grolog.with {
            author 'The story', 'John', 1994
            author 'Book 10', 'Mary', 2014
        }

        def book = new Var()

        def result = grolog.query( 'author', book, 'John', new Var() )
        assert result.advance()
        assert book.get() == 'The story'
        assert !result.advance()

        result = grolog.query( 'author', book, 'Mary', 2014 )
        assert result.advance()
        assert book.get() == 'Book 10'
        assert !result.advance()

        result = grolog.query( 'author', book, 'John', 1988 ) // wrong year
        assert !result.advance()
    }

    void testSimpleUnification() {
        def grolog = new Grolog()

        grolog.with {
            married 'Renato'
            married 'John'
            happy( A ).iff { married( A ) }
        }

        def var = new Var()
        def result = grolog.query( 'happy', var )

        assert result.advance()
        assert var.get() == 'Renato'
        assert result.advance()
        assert var.get() == 'John'
        assert !result.advance()
    }

    void testUseOfIntermediateUnboundVariableInCondition() {
        def grolog = new Grolog()

        grolog.with {
            loves 'Joe', 'Julia'
            loves 'Peter', 'Julia'
            loves 'Julia', 'Joe'
            jealous( A, B ).iff { loves( A, C ); loves( B, C ) }
        }

        assert grolog.query( 'jealous', 'Joe', 'Peter' ).exists
        assert grolog.query( 'jealous', 'Peter', 'Joe' ).exists
        assert !grolog.query( 'jealous', 'Joe', 'Julia' ).exists
        assert !grolog.query( 'jealous', 'Julia', 'Joe' ).exists
        assert !grolog.query( 'jealous', 'Mark', 'Joe' ).exists
    }

}
