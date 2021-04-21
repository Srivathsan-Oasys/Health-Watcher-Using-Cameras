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

import com.example.healthwatchervitalsigns.vitals_checker_back_camera.math.MathSupplement.addmul
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.complex.ComplexUtils

/**
 * The mother of all filters. It contains the coefficients of all
 * filter stages as a sequence of 2nd order filters and the states
 * of the 2nd order filters which also imply if it's direct form I or II
 */
open class Cascade {
    // coefficients
    private var m_biquads: Array<Biquad?>? = null

    // the states of the filters
    private var m_states: Array<DirectFormAbstract?>? = null

    // number of biquads in the system
    var numBiquads = 0
        private set
    private var numPoles = 0
    fun getBiquad(index: Int): Biquad? {
        return m_biquads!![index]
    }

    fun reset() {
        for (i in 0 until numBiquads) m_states!![i]?.reset()
    }

    fun filter(`in`: Double): Double {
        var out = `in`
        for (i in 0 until numBiquads) {
            if (m_states!![i] != null) {
                out = m_states!![i]?.process1(out, m_biquads!![i])!!
            }
        }
        return out
    }

    fun response(normalizedFrequency: Double): Complex {
        val w = 2 * Math.PI * normalizedFrequency
        val czn1 = ComplexUtils.polar2Complex(1.0, -w)
        val czn2 = ComplexUtils.polar2Complex(1.0, -2 * w)
        var ch = Complex(1.0)
        var cbot = Complex(1.0)
        for (i in 0 until numBiquads) {
            val stage: Biquad = m_biquads!![i]!!
            var cb = Complex(1.0)
            var ct = Complex(stage.b0 / stage.a0)
            ct = addmul(ct, stage.b1 / stage.a0, czn1)
            ct = addmul(ct, stage.b2 / stage.a0, czn2)
            cb = addmul(cb, stage.a1 / stage.a0, czn1)
            cb = addmul(cb, stage.a2 / stage.a0, czn2)
            ch = ch.multiply(ct)
            cbot = cbot.multiply(cb)
        }
        return ch.divide(cbot)
    }

    fun applyScale(scale: Double) {
        // For higher order filters it might be helpful
        // to spread this factor between all the stages.
        if (m_biquads!!.isNotEmpty()) {
            m_biquads!![0]?.applyScale(scale)
        }
    }

    fun setLayout(proto: LayoutBase, filterTypes: Int) {
        numPoles = proto.numPoles
        numBiquads = (numPoles + 1) / 2
        m_biquads = arrayOfNulls<Biquad>(numBiquads)
        when (filterTypes) {
            DirectFormAbstract.DIRECT_FORM_I -> {
                m_states = Array(numBiquads) { null }
                var i = 0
                while (i < numBiquads) {
                    m_states!![i] = DirectFormI()
                    i++
                }
            }
            DirectFormAbstract.DIRECT_FORM_II -> {
                m_states = Array(numBiquads) { null }
                var i = 0
                while (i < numBiquads) {
                    m_states!![i] = DirectFormII()
                    i++
                }
            }
            else -> {
                m_states = Array(numBiquads) { null }
                var i = 0
                while (i < numBiquads) {
                    m_states!![i] = DirectFormII()
                    i++
                }
            }
        }
        for (i in 0 until numBiquads) {
            val p = proto.getPair(i)
            m_biquads!![i] = Biquad()
            p?.let { m_biquads!![i]?.setPoleZeroPair(it) }
        }
        applyScale(
            proto.normalGain
                    / response(proto.normalW / (2 * Math.PI)).abs()
        )
    }
}