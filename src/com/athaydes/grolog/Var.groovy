package com.athaydes.grolog

import groovy.transform.PackageScope

import java.util.concurrent.atomic.AtomicReference

/**
 * Placeholder for a Grolog query variable
 */
class Var {

    private final ref = new AtomicReference()

    @PackageScope
    void set( value ) {
        ref.set value
    }

    /**
     * @return the next match or null if there is no more matches
     */
    def get() {
        ref.get()
    }

}
