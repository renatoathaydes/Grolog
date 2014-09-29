package com.athaydes.grolog.internal

import com.athaydes.grolog.ArityException
import com.athaydes.grolog.InvalidDeclaration
import com.athaydes.grolog.Var

import java.util.concurrent.atomic.AtomicReference

import static java.util.Collections.emptyList

class Querier {

    private final Map<String, Set<Fact>> facts

    Querier( Map<String, Set<Fact>> facts ) {
        this.facts = facts
    }

    Set<Fact> allFacts() {
        facts.values().flatten()
    }

    def query( String q, Object[] args ) {
        verifyValidQuery q, args
        ensureAllVarsEmpty args
        queryInternal( q, args )
    }

    protected queryInternal( String q, Object[] queryArgs ) {
        println "Query: $q $queryArgs"
        def possibilities = new LinkedList<Fact>( facts[ q ] ?: emptyList() )

        if ( !possibilities ) return false

        if ( !queryArgs.any { it instanceof Var } ) {
            return possibilities.any { simpleQuerySatisfies( it, queryArgs ) }
        }

        new Iterable() {
            def nextElement = null
            boolean hasNextCalledLast = false

            Iterator iterator() {
                [ hasNext: {
                    if ( hasNextCalledLast ) return nextElement != null
                    hasNextCalledLast = true
                    ( nextElement = unify( possibilities, queryArgs ) ) != null
                },
                  next   : {
                      if ( !hasNextCalledLast && !hasNext() ) throw new NoSuchElementException()
                      hasNextCalledLast = false
                      nextElement
                  } ] as Iterator
            }
        }
    }

    protected boolean simpleQuerySatisfies( Fact fact, Object[] queryArgs ) {
        def conditionsSatisfied = {
            List<UnboundedFact> correlatedClauses = [ ]
            for ( clause in fact.condition.allClauses() ) {
                def resolvedArgs = resolvedArgs( fact, clause, queryArgs )
                def clauseAsFact = Inserter.createFact( clause.name, resolvedArgs )
                if ( clauseAsFact instanceof UnboundedFact ) {
                    correlatedClauses << clauseAsFact
                } else {
                    if ( correlatedClauses )
                        throw new InvalidDeclaration( "Unbounded clause(s) must not be followed by definite clause: $fact" )
                    if ( !( clauseAsFact in facts[ clause.name ] ) )
                        return false
                }
            }
            satisfiedCorrelatedClauses correlatedClauses, [ : ]
        }
        def simpleQueryArgsMatch = {
            fact instanceof UnboundedFact || matchSuccessful( fact, queryArgs )
        }
        simpleQueryArgsMatch() && conditionsSatisfied()
    }

    boolean satisfiedCorrelatedClauses( List<UnboundedFact> correlatedClauses, Map paramsByName ) {
        if ( !correlatedClauses ) return true

        def clause = correlatedClauses.first()

        // replace UnboundedVars in args with Vars or concrete instances if found any in earlier iterations
        def params = clause.args.collect {
            it instanceof UnboundedVar ? ( paramsByName[ it.name ] ?: new Var() ) : it
        }

        def queryResults = queryInternal( clause.name, params as Object[] )

        if ( !( queryResults instanceof Iterable ) ) return queryResults
        queryResults = queryResults.iterator()

        if ( !queryResults.hasNext() ) return false

        def unboundedVarNames = clause.args.toList().findResults {
            it instanceof UnboundedVar && paramsByName[ it.name ] == null ? it.name : null
        }

        if ( !unboundedVarNames ) return true

        while ( queryResults.hasNext() ) {
            queryResults.next()
            def visitedVars = params.findAll { it instanceof Var } as LinkedList<Var>
            for ( varName in unboundedVarNames ) {
                def param = visitedVars.removeFirst()
                assert param.get() != null, 'Satisfied Var with null'
                paramsByName[ varName ] = param.get()
            }
            if ( !satisfiedCorrelatedClauses( correlatedClauses.tail(), paramsByName ) )
                return false
        }

        return true
    }

    Map<String, Object> boundedArgs( UnboundedFact fact, Object[] queryArgs ) {
        def boundedArgs = [ : ]
        fact.args.eachWithIndex { arg, int index ->
            if ( arg instanceof UnboundedVar ) {
                boundedArgs.put( arg.name, queryArgs ? queryArgs[ index ] : '_' )
            }
        }
        boundedArgs.asImmutable()
    }

    private void verifyValidQuery( String q, Object[] queryArgs ) {
        assert q, 'A query must be provided'
        def possibilities = facts[ q ]
        if ( possibilities && queryArgs && queryArgs.size() != possibilities.first().args.size() ) {
            throw new ArityException( q, possibilities.first().args.size() )
        }
    }

    private void ensureAllVarsEmpty( Object[] args ) {
        args.findAll { it instanceof Var }.each { Var var -> var.set( null ) }
    }

    protected static unify( LinkedList<Fact> possibilities, Object[] queryArgs ) {
        println "Trying to unify possibilities $possibilities with $queryArgs"
        while ( possibilities ) {
            if ( matchSuccessful( possibilities.removeFirst(), queryArgs ) ) return true
        }
        return null
    }

    private static boolean matchSuccessful( Fact fact, Object[] queryArgs ) {
        assert fact.args.size() == queryArgs.size(), "Fact and queryArgs have different sizes"
        for ( argPair in [ fact.args, queryArgs ].transpose() ) {
            def factArg = argPair[ 0 ]
            def queryArg = argPair[ 1 ]
            if ( !( factArg instanceof UnboundedVar ) ) {
                if ( queryArg instanceof Var ) queryArg.set factArg
                else if ( !Var._.is( queryArg ) && factArg != queryArg ) return false
            }
        }
        println "Unification of $fact with $queryArgs successful"
        return true
    }

    protected Object[] resolvedArgs( Fact fact, Fact cond, Object[] queryArgs ) {
        if ( fact instanceof UnboundedFact ) {
            def boundedArgs = boundedArgs( fact, queryArgs )
            def res = cond.args.collect { arg ->
                if ( arg instanceof UnboundedVar ) {
                    boundedArgs[ arg.name ] ?: arg
                } else {
                    arg
                }
            }
            println "Resolved args $res"
            res
        } else {
            cond.args
        }
    }

    private void ensureListsWithOneItemAreLifted( List<List> maybeMatches ) {
        for ( maybeMatch in maybeMatches ) {
            def current = maybeMatch[ 0 ]
            if ( current instanceof AtomicReference && current.get() && current.get().size() == 1 ) {
                current.set( ( current.get() as List )[ 0 ] )
            }
        }
    }

}

class ConditionQuerier extends Querier {

    ConditionQuerier( Map<String, Set<Fact>> facts ) {
        super( facts )
    }

}