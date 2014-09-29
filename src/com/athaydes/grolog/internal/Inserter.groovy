package com.athaydes.grolog.internal

import com.athaydes.grolog.ArityException

class Inserter {

    private final Map<String, Set<Fact>> facts

    Inserter( Map<String, Set<Fact>> facts ) {
        this.facts = facts
    }

    void clear() {
        facts.clear()
    }

    def addVar( String name ) {
        if ( name.toCharArray()[ 0 ].upperCase ) {
            return new UnboundedVar( name )
        }
        addFact( name, Collections.emptyList() as Object[] )
    }

    Fact addFact( String name, Object[] args ) {
        addFact createFact( name, args )
    }

    static Fact createFact( String name, Object[] args ) {
        args.any { it instanceof UnboundedVar } ? new UnboundedFact( name, args ) : new Fact( name, args )
    }

    /**
     * Adds a single Fact or all Facts in a given Collection of Facts
     * @param fact Fact or Collection<Fact>
     * @return fact given
     */
    def addFact( fact ) {
        switch ( fact ) {
            case Fact:
                fact = fact as Fact // no warnings
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

}
