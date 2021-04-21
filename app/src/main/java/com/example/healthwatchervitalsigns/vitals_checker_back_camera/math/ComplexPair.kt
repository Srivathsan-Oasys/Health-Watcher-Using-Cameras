/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Copyright (c) 2009 by Vinnie Falco
 *  Copyright (c) 2016 by Bernd Porr
 */
package com.example.healthwatchervitalsigns.vitals_checker_back_camera.math

import org.apache.commons.math3.complex.Complex

/**
 * A complex pair
 */
class ComplexPair {
    @JvmField
    var first: Complex

    @JvmField
    var second: Complex

    internal constructor(
        c1: Complex,
        c2: Complex
    ) {
        first = c1
        second = c2
    }

    internal constructor(c1: Complex) {
        first = c1
        second = Complex(0.0, 0.0)
    }

    val isConjugate: Boolean
        get() = second.equals(first.conjugate())
    val isReal: Boolean
        get() = first.imaginary === 0.0 && second.imaginary === 0.0

    // Returns true if this is either a conjugate pair,
    // or a pair of reals where neither is zero.
    val isMatchedPair: Boolean
        get() = if (first.imaginary !== 0.0) second == first.conjugate()
        else second.imaginary === 0.0 && second.real !== 0.0 && first.real !== 0.0

    fun is_nan(): Boolean {
        return first.isNaN || second.isNaN
    }
}