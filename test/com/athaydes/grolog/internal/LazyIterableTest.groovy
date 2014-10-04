package com.athaydes.grolog.internal

/**
 *
 */
class LazyIterableTest extends GroovyTestCase {

    def nextCalls = 0
    def iterable = new LazyIterable() {
        def computeNext() { ++nextCalls }
    }

    def iter = iterable.iterator()

    void testHasNextCalledMultipleTimes() {
        5.times { iter.hasNext() }
        iter.next()
        44.times { iter.hasNext() }

        assert nextCalls == 2
    }

    void testNextCalledMultipleTimes() {
        5.times { iter.next() }

        assert nextCalls == 5
    }

    void testErrorIfNextCalledButHasNotNext() {
        def i = 1
        def iterable = [ computeNext: { i-- ?: null } ] as LazyIterable // returns true only first time called
        def iter = iterable.iterator()

        assert iter.hasNext()
        iter.next()

        shouldFail( NoSuchElementException ) {
            iter.next()
        }

        assert !iter.hasNext()
        shouldFail( NoSuchElementException ) {
            iter.next()
        }

        assert i == -1
    }

    void testDifferentIteratorsCanIterateSameIterable() {
        def createIterable = {
            new LazyIterable() {
                LinkedList responses = [ 10, 20, null ]

                def computeNext() { responses.removeFirst() }
            }
        }

        final iterable = createIterable()

        def iter1 = iterable.iterator()

        assert iter1.hasNext()
        assert iter1.next() == 10
        assert iter1.hasNext()
        assert iter1.next() == 20
        assert !iter1.hasNext()

        shouldFail( NoSuchElementException ) {
            iter1.next()
        }

        def iter2 = iterable.iterator()

        // iter2 has the same view of the iterable as iter1
        shouldFail( NoSuchElementException ) {
            iter2.next()
        }

    }


}
