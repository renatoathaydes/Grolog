package com.athaydes.grolog.internal

import groovy.transform.PackageScope

abstract class LazyIterable implements Iterable {

    @PackageScope
    abstract computeNext()

    @Override
    Iterator iterator() {
        new LazyIterator( iterable: this )
    }
}

class LazyIterator implements Iterator {

    private LazyIterable iterable
    private boolean hasNextCalledLast = false
    private boolean hadNext = true
    private nextElement

    @Override
    boolean hasNext() {
        if ( hasNextCalledLast ) {
            hadNext
        } else {
            hasNextCalledLast = true
            nextElement = iterable.computeNext()
            hadNext = ( nextElement != null )
        }
    }

    @Override
    def next() {
        if ( ( !hasNextCalledLast && !hasNext() ) || !hadNext ) {
            throw new NoSuchElementException()
        }
        hasNextCalledLast = false
        nextElement
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException()
    }

}
