package com.athaydes.grolog

import com.athaydes.grolog.internal.Fact
import com.athaydes.grolog.internal.UnboundedFact
import com.athaydes.grolog.internal.UnboundedVar
import groovy.transform.PackageScope

@PackageScope
class Inserter {

    private final Map<String, Set<Fact>> facts
    protected final Set<UnboundedVar> unboundedVars

    Inserter( Map<String, Set<Fact>> facts, Set<UnboundedVar> unboundedVars ) {
        this.facts = facts
        this.unboundedVars = unboundedVars
    }

    void clear() {
        facts.clear()
        unboundedVars.clear()
    }

    def addVar( String name ) {
        if ( name.toCharArray()[ 0 ].upperCase ) {
            def var = new UnboundedVar( name )
            unboundedVars << var
            return var
        }
        addFact( name, Collections.emptyList() as Object[] )
    }

    protected Fact addFact( String name, Object[] args ) {
        def fact = unboundedVars ? new UnboundedFact( name, args, drain( unboundedVars ) ) : new Fact( name, args )
        addFact fact
    }

    /**
     * Adds a single Fact or all Facts in a given Collection of Facts
     * @param fact Fact or Collection<Fact>
     * @return fact given
     */
    def addFact( fact ) {
        switch ( fact ) {
            case Fact:
                def allFacts = facts.get( fact.name )
                if ( allFacts && allFacts.first().args.size() != fact.args.size() ) {
                    throw new ArityException( fact.name, allFacts.first().args.size() )
                } else if ( allFacts == null ) {
                    facts.put( fact.name, allFacts = [ ] as Set )
                }
                allFacts << fact
                return fact
            case Collection:
                for ( f in fact ) {
                    addFact f
                }
                return fact
            default: throw new RuntimeException( "Trying to add unexpected Fact type ${fact.class.name}" )
        }
    }

    protected Set drain( Set set ) {
        def copy = new LinkedHashSet( set )
        set.clear()
        copy
    }

}
