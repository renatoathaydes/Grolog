package com.athaydes.grolog.internal

import com.athaydes.grolog.ConditionGrolog
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.ToString

import static java.util.Collections.emptySet

@ToString( includePackage = false, includeFields = true,
        includes = [ 'name', 'args', 'condition' ] )
@EqualsAndHashCode( includes = [ 'name', 'args' ] )
class Fact {

    final String name
    final Object[] args
    final Condition condition

    Fact( String name, args, Condition condition = new Condition( emptySet() ) ) {
        this.name = name
        this.args = args
        this.condition = condition
    }

}

class UnboundedFact extends Fact {

    UnboundedFact( String name, Object args, Set<String> unboundedVars ) {
        super( name, args, new Condition( unboundedVars ) )
    }

    Map<String, Object> boundedArgs( Object[] queryArgs ) {
        def boundedArgs = [ : ]
        args.eachWithIndex { arg, int index ->
            if ( arg instanceof UnboundedVar ) {
                boundedArgs.put( arg.name, queryArgs ? queryArgs[ index ] : '_' )
            }
        }
        boundedArgs
    }

}

class Condition {

    private final ConditionGrolog grolog

    Condition( Set<String> unboundedVars ) {
        this.grolog = new ConditionGrolog( unboundedVars )
    }

    void iff( Closure clausesCallback ) {
        grolog.with clausesCallback
    }

    boolean hasClauses() {
        grolog.allFacts()
    }

    Set<Fact> allClauses() {
        grolog.allFacts()
    }

    @Override
    String toString() {
        def statements = grolog.allFacts()
        if ( statements ) {
            "Cond($statements)"
        } else {
            '_'
        }
    }

}

@Immutable
@ToString( includePackage = false )
class UnboundedVar {
    String name
}