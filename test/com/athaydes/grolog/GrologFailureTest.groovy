package com.athaydes.grolog

/**
 *
 */
class GrologFailureTest extends GroovyTestCase {

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

    void testUnboundedFactsWithoutConditionAreNotAccepted() {
        def grolog = new Grolog()

        shouldFail( InvalidDeclaration ) {
            grolog.with {
                something A
            }
        }
        shouldFail( InvalidDeclaration ) {
            grolog.with {
                whatever A, B
            }
        }
    }

}

