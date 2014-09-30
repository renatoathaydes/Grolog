package com.athaydes.grolog

import groovy.transform.PackageScope
import groovy.transform.ToString

import java.util.concurrent.atomic.AtomicReference

/**
 * Placeholder for a Grolog query variable
 */
@ToString(includePackage = false, includeFields = true)
class Var {

    /**
     * Ignorable Var -> will match any argument
     */
    static final _ = new Object()

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
