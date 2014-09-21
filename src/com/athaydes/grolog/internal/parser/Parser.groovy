package com.athaydes.grolog.internal.parser

import com.athaydes.grolog.Grolog

interface Parser {

    Grolog from( InputStream input )

    Grolog from( InputStream input, String charset )

}
