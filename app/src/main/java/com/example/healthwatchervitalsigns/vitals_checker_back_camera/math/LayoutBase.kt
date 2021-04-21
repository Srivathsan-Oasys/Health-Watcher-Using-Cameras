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
 * Digital/analogue filter coefficient storage space organising the
 * storage as PoleZeroPairs so that we have as always a 2nd order filter
 */
open class LayoutBase {
    var numPoles: Int
        private set
    private val m_pair: Array<PoleZeroPair?>?
    var normalW = 0.0
        private set
    var normalGain = 0.0
        private set

    constructor(pairs: Array<PoleZeroPair?>) {
        numPoles = pairs.size * 2
        m_pair = pairs
    }

    constructor(numPoles: Int) {
        this.numPoles = 0
        if (numPoles % 2 == 1) {
            m_pair = arrayOfNulls<PoleZeroPair>(numPoles / 2 + 1)
        } else {
            m_pair = arrayOfNulls<PoleZeroPair>(numPoles / 2)
        }
    }

    fun reset() {
        numPoles = 0
    }

    fun add(pole: Complex?, zero: Complex?) {
        m_pair!![numPoles / 2] = PoleZeroPair(pole, zero)
        ++numPoles
    }

    fun addPoleZeroConjugatePairs(pole: Complex?, zero: Complex?) {
        if (pole == null) println("LayoutBase addConj() pole == null")
        if (zero == null) println("LayoutBase addConj() zero == null")
        if (m_pair == null) println("LayoutBase addConj() m_pair == null")
        m_pair!![numPoles / 2] = PoleZeroPair(
            pole,
            zero,
            pole?.conjugate(),
            zero?.conjugate()
        )
        numPoles += 2
    }

    fun add(poles: ComplexPair, zeros: ComplexPair) {
        println("LayoutBase add() numPoles=" + numPoles)
        m_pair!![numPoles / 2] = PoleZeroPair(
            poles.first, zeros.first,
            poles.second, zeros.second
        )
        numPoles += 2
    }

    fun getPair(pairIndex: Int): PoleZeroPair? {
        return m_pair!![pairIndex]
    }

    fun setNormal(w: Double, g: Double) {
        normalW = w
        normalGain = g
    }
}