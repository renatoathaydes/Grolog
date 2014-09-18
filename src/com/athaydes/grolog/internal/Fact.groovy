package com.athaydes.grolog.internal

import com.athaydes.grolog.ConditionGrolog
import com.athaydes.grolog.Grolog
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.ToString

import java.util.concurrent.atomic.AtomicReference

@ToString( includePackage = false, includeFields = true,
        includes = [ 'name', 'args', 'condition' ] )
@EqualsAndHashCode( includes = [ 'name', 'args' ] )
class Fact {

    final String name
    final Object[] args
    final Condition condition

    Fact( String name, args, Set<String> unboundedVars ) {
        this.name = name
        this.args = args
        this.condition = new Condition( unboundedVars )
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

    boolean satisfiedBy( Grolog other ) {
        def truths = this.grolog.maybeTrueFacts()
        truths.any { Fact fact -> fact.args.any { it instanceof AtomicReference } } ||
                truths.every { Fact fact ->
                    other.queryInternal( false, fact.name, fact.args )
                }
    }

    def unboundedVarResolves( Grolog other, Object[] queryArgs ) {
        def truths = this.grolog.maybeTrueFacts()
        truths.any { Fact fact -> fact.args.any { it instanceof AtomicReference } } ||
                truths.every { Fact fact ->
                    other.queryInternal( false, fact.name, queryArgs )
                }
    }

    @Override
    String toString() {
        def statements = grolog.maybeTrueFacts()
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