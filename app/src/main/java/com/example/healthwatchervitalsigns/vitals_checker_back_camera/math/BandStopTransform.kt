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

/**
 * Transforms from an analogue lowpass filter to a digital bandstop filter
 */
class BandStopTransform(
    fc: Double,
    fw: Double,
    digital: LayoutBase,
    analog: LayoutBase
) {
    private var wc: Double
    private var wc2: Double
    private val a: Double
    private val b: Double
    private val a2: Double
    private val b2: Double
    private fun transform(c: Complex): ComplexPair {
        var c = c
        c = if (c.isInfinite) Complex(-1.0) else Complex(1.0).add(c)
            .divide(Complex(1.0).subtract(c)) // bilinear
        var u = Complex(0.0)
        u = addmul(u, 4 * (b2 + a2 - 1), c)
        u = u.add(8 * (b2 - a2 + 1))
        u = u.multiply(c)
        u = u.add(4 * (a2 + b2 - 1))
        u = u.sqrt()
        var v = u.multiply(-.5)
        v = v.add(a)
        v = addmul(v, -a, c)
        u = u.multiply(.5)
        u = u.add(a)
        u = addmul(u, -a, c)
        var d = Complex(b + 1)
        d = addmul(d, b - 1, c)
        return ComplexPair(u.divide(d), v.divide(d))
    }

    init {
        digital.reset()
        val ww = 2 * Math.PI * fw
        wc2 = 2 * Math.PI * fc - ww / 2
        wc = wc2 + ww

        // this is crap
        if (wc2 < 1e-8) wc2 = 1e-8
        if (wc > Math.PI - 1e-8) wc = Math.PI - 1e-8
        a = Math.cos((wc + wc2) * .5) /
                Math.cos((wc - wc2) * .5)
        b = Math.tan((wc - wc2) * .5)
        a2 = a * a
        b2 = b * b
        val numPoles = analog.numPoles
        val pairs = numPoles / 2
        for (i in 0 until pairs) {
            val pair = analog.getPair(i)
            val p = transform(pair!!.poles!!.first)
            val z = transform(pair.zeros!!.first)
            digital.addPoleZeroConjugatePairs(p.first, z.first)
            digital.addPoleZeroConjugatePairs(p.second, z.second)
        }
        if (numPoles and 1 == 1) {
            val poles = transform(analog.getPair(pairs)!!.poles!!.first)
            val zeros = transform(analog.getPair(pairs)!!.zeros!!.first)
            digital.add(poles, zeros)
        }
        if (fc < 0.25) digital.setNormal(Math.PI, analog.normalGain) else digital.setNormal(
            0.0,
            analog.normalGain
        )
    }
}