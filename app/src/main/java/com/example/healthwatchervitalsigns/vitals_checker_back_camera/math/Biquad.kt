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
 * Contains the coefficients of a 2nd order digital filter with two poles and two zeros
 */
class Biquad {
    var a0 = 0.0
    var m_a1 = 0.0
    var m_a2 = 0.0
    var m_b1 = 0.0
    var m_b2 = 0.0
    var m_b0 = 0.0
    val a1: Double
        get() = m_a1 * a0
    val a2: Double
        get() = m_a2 * a0
    val b0: Double
        get() = m_b0 * a0
    val b1: Double
        get() = m_b1 * a0
    val b2: Double
        get() = m_b2 * a0

    fun response(normalizedFrequency: Double): Complex {
        val a0 = a0
        val a1 = a1
        val a2 = a2
        val b0 = b0
        val b1 = b1
        val b2 = b2
        val w = 2 * Math.PI * normalizedFrequency
        val czn1 = ComplexUtils.polar2Complex(1.0, -w)
        val czn2 = ComplexUtils.polar2Complex(1.0, -2 * w)
        var ch = Complex(1.0)
        var cbot = Complex(1.0)
        var ct = Complex(b0 / a0)
        var cb = Complex(1.0)
        ct = addmul(ct, b1 / a0, czn1)
        ct = addmul(ct, b2 / a0, czn2)
        cb = addmul(cb, a1 / a0, czn1)
        cb = addmul(cb, a2 / a0, czn2)
        ch = ch.multiply(ct)
        cbot = cbot.multiply(cb)
        return ch.divide(cbot)
    }

    fun setCoefficients(
        a0: Double, a1: Double, a2: Double,
        b0: Double, b1: Double, b2: Double
    ) {
        this.a0 = a0
        m_a1 = a1 / a0
        m_a2 = a2 / a0
        m_b0 = b0 / a0
        m_b1 = b1 / a0
        m_b2 = b2 / a0
    }

    fun setOnePole(pole: Complex, zero: Complex) {
        val a0 = 1.0
        val a1 = -pole.real
        val a2 = 0.0
        val b0 = -zero.real
        val b1 = 1.0
        val b2 = 0.0
        setCoefficients(a0, a1, a2, b0, b1, b2)
    }

    fun setTwoPole(
        pole1: Complex, zero1: Complex,
        pole2: Complex, zero2: Complex
    ) {
        val a0 = 1.0
        val a1: Double
        val a2: Double
        if (pole1.imaginary != 0.0) {
            a1 = -2 * pole1.real
            a2 = pole1.abs() * pole1.abs()
        } else {
            a1 = -(pole1.real + pole2.real)
            a2 = pole1.real * pole2.real
        }
        val b0 = 1.0
        val b1: Double
        val b2: Double
        if (zero1.imaginary != 0.0) {
            b1 = -2 * zero1.real
            b2 = zero1.abs() * zero1.abs()
        } else {
            b1 = -(zero1.real + zero2.real)
            b2 = zero1.real * zero2.real
        }
        setCoefficients(a0, a1, a2, b0, b1, b2)
    }

    fun setPoleZeroForm(bps: BiquadPoleState) {
        setPoleZeroPair(bps)
        applyScale(bps.gain)
    }

    fun setIdentity() {
        setCoefficients(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    }

    fun applyScale(scale: Double) {
        m_b0 *= scale
        m_b1 *= scale
        m_b2 *= scale
    }

    fun setPoleZeroPair(pair: PoleZeroPair) {
        if (pair.isSinglePole) {
            setOnePole(pair.poles?.first ?: Complex.ZERO, pair.zeros?.first ?: Complex.ZERO)
        } else {
            setTwoPole(
                pair.poles?.first ?: Complex.ZERO,
                pair.zeros?.first ?: Complex.ZERO,
                pair.poles?.second ?: Complex.ZERO,
                pair.zeros?.second ?: Complex.ZERO
            )
        }
    }
}