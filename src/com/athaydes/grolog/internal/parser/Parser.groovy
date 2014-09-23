package com.athaydes.grolog.internal.parser

import com.athaydes.grolog.Grolog

interface Parser {

    Grolog parsePredicates( InputStream input )

    Grolog parsePredicates( InputStream input, String charset )

    def parseQuery( String query )

}
