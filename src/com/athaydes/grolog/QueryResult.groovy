package com.athaydes.grolog

import static com.athaydes.grolog.QueryResult.ResultType.*

/**
 *
 */
class QueryResult {

    static QueryResult MultipleBindings( Iterable results ) {
        new QueryResult( MULTIPLE_BINDINGS, results )
    }

    static enum ResultType {
        MULTIPLE_BINDINGS,
        TRUE_FALSE,
        ERROR
    }

    static QueryResult TrueFalse( boolean result ) {
        new QueryResult( TRUE_FALSE, null, result )
    }

    static QueryResult Error( String errorMessage ) {
        new QueryResult( ERROR, null, false, errorMessage )
    }

    ResultType resultType

    private Iterable results
    private boolean result
    private String errorMessage

    private Iterator multipleBindingsIterator

    private QueryResult( ResultType resultType, Iterable results, boolean result = false, String errorMessage = null ) {
        this.resultType = resultType
        this.results = results
        this.result = result
        this.errorMessage = errorMessage
        if ( resultType == MULTIPLE_BINDINGS ) {
            restart()
        }
        println this
    }

    /**
     * MUTIPLE_BINDINGS - advance to next binding.
     * @return true if there is a next binding, false otherwise.
     * @throws UnsupportedOperationException if called with an instance that is not MULTIPLE_BINDINGS.
     */
    boolean advance() {
        assertType MULTIPLE_BINDINGS, 'advance()'
        if ( multipleBindingsIterator.hasNext() ) {
            multipleBindingsIterator.next()
            return true
        }
        return false
    }

    /**
     * MUTIPLE_BINDINGS - starts iteration over multiple bindings again.
     * @throws UnsupportedOperationException if called with an instance that is not MULTIPLE_BINDINGS.
     */
    void restart() {
        assertType MULTIPLE_BINDINGS, 'restart()'
        multipleBindingsIterator = results.iterator()
    }

    /**
     * TRUE_FALSE
     * @return true if a predicate satisfying the query exists, false otherwise.
     * @throws UnsupportedOperationException if called with an instance that is not TRUE_FALSE.
     */
    boolean isExists() {
        assertType TRUE_FALSE, 'isExists()'
        result
    }

    /**
     * ERROR
     * @return the error message
     * @throws UnsupportedOperationException if called with an instance that is not ERROR.
     */
    String getErrorMessage() {
        assertType ERROR, 'getErrorMessage()'
        errorMessage
    }

    @Override
    public String toString() {
        switch ( resultType ) {
            case TRUE_FALSE: return "QueryResult(${result})"
            case MULTIPLE_BINDINGS: return "QueryResult(${results})"
            case ERROR: return "QueryResult(${errorMessage})"
        }
    }

    private void assertType( ResultType expected, String methodName ) {
        if ( this.resultType != expected ) {
            throw new UnsupportedOperationException( "QueryResult of type $resultType does not support $methodName" )
        }
    }


}
