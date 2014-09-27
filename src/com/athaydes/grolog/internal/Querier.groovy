package com.athaydes.grolog.internal

import com.athaydes.grolog.ArityException
import com.athaydes.grolog.Var

import java.util.concurrent.atomic.AtomicReference

import static java.util.Collections.emptyList

class Querier {

    private final Map<String, Set<Fact>> facts

    Querier( Map<String, Set<Fact>> facts ) {
        this.facts = facts
    }

    def query( String q, Object[] args ) {
        verifyValidQuery q, args
        ensureAllVarsEmpty args
        queryInternal( q, args )
    }

    protected queryInternal( String q, Object[] queryArgs ) {
        def possibilities = new ArrayList<Fact>( facts[ q ] ?: emptyList() )

        if ( !possibilities ) return false

        if ( !queryArgs.any { it instanceof Var } ) {
            return possibilities.any { simpleQuerySatisfies( it, queryArgs ) }
        }

        new Iterable() {
            def nextElement
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
            for ( clause in fact.condition.allClauses() ) {
                def queriedFact = new Fact( clause.name, resolvedArgs( fact, clause, queryArgs ) )
                if ( !( queriedFact in facts[ clause.name ] ) ) return false
            }
            return true
        }
        def simpleQueryArgsMatch = {
            fact instanceof UnboundedFact || fact.args.toList() == queryArgs.toList()
        }
        simpleQueryArgsMatch() && conditionsSatisfied()
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

    Set<Fact> allPossibilities( String name = null, Object[] queryArgs = [ ] ) {
        filterFacts( name ) { Fact fact ->
            ( fact instanceof UnboundedFact ) ? fact : provenFactOrNull( fact, queryArgs )
        }
    }

    Set<Fact> provenFacts( String name = null, Object[] queryArgs = [ ] ) {
        filterFacts( name ) { Fact fact ->
            provenFactOrNull( fact, queryArgs )
        }
    }

    Set<Fact> allFacts() {
        facts.values().flatten()
    }

    protected Fact provenFactOrNull( Fact fact, Object[] queryArgs, boolean resolveUnboundedAndConditionalFacts ) {
        if ( !resolveUnboundedAndConditionalFacts && ( fact instanceof UnboundedFact || fact.condition.hasClauses() ) ) {
            return null
        }
        def conditions = fact.condition.allClauses()
        if ( !conditions ) {
            return ( fact instanceof UnboundedFact ) ? null : match( [ fact ] as Set, queryArgs, 0, [ ] ) ? fact : null
        }

        Map<String, Object> boundedArgs = ( fact instanceof UnboundedFact ) ? fact.boundedArgs( queryArgs ) : null
        def conditionsSatisfied = conditions.every { Fact cond -> conditionSatisfied( cond, boundedArgs ) }

        if ( conditionsSatisfied ) {
            //FIXME
            return boundedArgs ? new Fact( fact.name, resolvedArgs( fact.args, boundedArgs ) ) : fact
        } else {
            return null
        }
    }

    protected boolean conditionSatisfied( Fact cond, Map<String, Object> boundedArgs ) {
        Object[] actualArgs = boundedArgs ? resolvedArgs( cond.args, boundedArgs ) : cond.args
        queryInternal( cond.name, actualArgs ) //TODO here we need to resolve fact, use another method
    }

    private Set<Fact> filterFacts( String name, Closure<Fact> filter ) {
        if ( name ) {
            facts.get( name )?.findResults filter
        } else {
            facts.values().flatten().findResults filter
        }
    }

    private void verifyValidQuery( String q, Object[] queryArgs ) {
        assert q, 'A query must be provided'
        def possibilities = facts[ q ]
        if ( possibilities && queryArgs && queryArgs.size() != possibilities.first().args.size() ) {
            throw new ArityException( q, possibilities.first().args.size() )
        }
    }

    private void ensureAllVarsEmpty( Object[] args ) {
        args.findAll { it instanceof AtomicReference }.each { it.set( null ) }
    }

    protected static unify( possibilities, queryArgs ) {
        null
    }

    protected Object[] resolvedArgs( Fact fact, Fact cond, Object[] queryArgs ) {
        if ( fact instanceof UnboundedFact ) {
            def boundedArgs = boundedArgs( fact, queryArgs )
            def res = cond.args.collect { arg ->
                if ( arg instanceof UnboundedVar ) {
                    boundedArgs[ arg.name ]
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

    private match( Set<Fact> facts, Object[] queryArgs, int index,
                   List maybeMatches ) {
        def candidateFacts = facts.findAll { it.args.size() >= index }
        if ( index >= queryArgs.size() ) {
            return ( candidateFacts ? bindMatches( maybeMatches, candidateFacts, queryArgs ) : true )
        }

        def queryArg = queryArgs[ index ] instanceof AtomicReference
        def matchingFacts = null

        if ( queryArg ) {
            maybeMatches << [ queryArgs[ index ], candidateFacts, index ]
        } else {
            matchingFacts = factMatches( candidateFacts, queryArgs, index )
        }

        if ( queryArg || matchingFacts ) {
            match( queryArg ? candidateFacts : matchingFacts, queryArgs, index + 1, maybeMatches )
        } else {
            false
        }
    }

    protected Set<Fact> factMatches( Set<Fact> facts, Object[] queryArgs, int argIndex ) {
        facts.findAll { it.args[ argIndex ] instanceof UnboundedVar || it.args[ argIndex ] == queryArgs[ argIndex ] }
    }

    private bindMatches( List<List> maybeMatches, Set<Fact> candidateFacts, Object[] queryArgs ) {
        if ( !candidateFacts ) {
            println "No candidate facts"
            return false
        }

        if ( !maybeMatches ) {
            println "No maybe matches! Candidates: ${candidateFacts}"
            return satisfiedFacts( candidateFacts, queryArgs )
        }

        for ( maybeMatch in maybeMatches ) {
            maybeMatchQuery( maybeMatch, candidateFacts.findAll { provenFactOrNull( it, [ ] as Object[], false ) } )
        }

        ensureListsWithOneItemAreLifted maybeMatches
        maybeMatches.every { maybeMatch ->
            def current = maybeMatch[ 0 ]
            ( current instanceof AtomicReference<List> ? current.get() != null : true )
        }
    }

    protected satisfiedFacts( Set<Fact> candidateFacts, Object[] queryArgs ) {
        for ( candidate in candidateFacts ) {
            if ( candidate instanceof UnboundedFact ) {
                for ( clause in candidate.condition.allClauses() ) {
                    def satisfied = provenFacts( clause.name, queryArgs )
                    println "Checking if clause is satisfied $clause: $queryArgs ? $satisfied"
                    if ( !satisfied ) {
                        println "Nope"
                        return false
                    }
                }
            }
        }
        return true
    }

    private void maybeMatchQuery( List maybeMatch, Set<Fact> trueFacts ) {
        def current = maybeMatch[ 0 ] as AtomicReference<List>
        def maybeFacts = maybeMatch[ 1 ] as Set<Fact>
        def index = maybeMatch[ 2 ] as int

        def currentValue = maybeFacts.findAll { it in trueFacts }.collect { it.args[ index ] }

        if ( currentValue.any { it instanceof AtomicReference } ) {
            return
        }

        if ( current.get() == null ) {
            current.set currentValue
        } else {
            current.get().addAll currentValue
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

    @Override
    protected Set<Fact> factMatches( Set<Fact> facts, Object[] queryArgs, int argIndex ) {
        facts.findAll { it.args[ argIndex ] == queryArgs[ argIndex ] }
    }

}