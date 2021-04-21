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
 * Transforms from an analogue bandpass filter to a digital bandstop filter
 */
class BandPassTransform(
    fc: Double, fw: Double, digital: LayoutBase,
    analog: LayoutBase
) {
    private var wc2: Double
    private var wc: Double
    private val a: Double
    private val b: Double
    private val a2: Double
    private val b2: Double
    private val ab: Double
    private val ab_2: Double
    private fun transform(c: Complex): ComplexPair {
        var c = c
        if (c.isInfinite) {
            return ComplexPair(Complex(-1.0), Complex(1.0))
        }
        c = Complex(1.0).add(c).divide(Complex(1.0).subtract(c)) // bilinear
        var v = Complex(0.0)
        v = MathSupplement.addmul(v, 4 * (b2 * (a2 - 1) + 1), c)
        v = v.add(8 * (b2 * (a2 - 1) - 1))
        v = v.multiply(c)
        v = v.add(4 * (b2 * (a2 - 1) + 1))
        v = v.sqrt()
        var u = v.multiply(-1)
        u = MathSupplement.addmul(u, ab_2, c)
        u = u.add(ab_2)
        v = MathSupplement.addmul(v, ab_2, c)
        v = v.add(ab_2)
        var d = Complex(0.0)
        d = MathSupplement.addmul(d, 2 * (b - 1), c).add(2 * (1 + b))
        return ComplexPair(u.divide(d), v.divide(d))
    }

    init {
        digital.reset()
        val ww = 2 * Math.PI * fw

        // pre-calcs
        wc2 = 2 * Math.PI * fc - ww / 2
        wc = wc2 + ww

        // what is this crap?
        if (wc2 < 1e-8) wc2 = 1e-8
        if (wc > Math.PI - 1e-8) wc = Math.PI - 1e-8
        a = Math.cos((wc + wc2) * 0.5) / Math.cos((wc - wc2) * 0.5)
        b = 1 / Math.tan((wc - wc2) * 0.5)
        a2 = a * a
        b2 = b * b
        ab = a * b
        ab_2 = 2 * ab
        val numPoles = analog.numPoles
        val pairs = numPoles / 2
        for (i in 0 until pairs) {
            val pair = analog.getPair(i)
            val p1 = pair!!.poles?.first?.let { transform(it) }
            val z1 = pair.zeros?.first?.let { transform(it) }
            digital.addPoleZeroConjugatePairs(p1?.first, z1?.first)
            digital.addPoleZeroConjugatePairs(p1?.second, z1?.second)
        }
        if (numPoles and 1 == 1) {
            val poles = analog.getPair(pairs)!!.poles?.first?.let { transform(it) }
            val zeros = analog.getPair(pairs)!!.zeros?.first?.let { transform(it) }
            if (poles != null && zeros != null) {
                digital.add(poles, zeros)
            }
        }
        val wn = analog.normalW
        digital.setNormal(
            2 * Math.atan(
                Math.sqrt(
                    Math.tan((wc + wn) * 0.5)
                            * Math.tan((wc2 + wn) * 0.5)
                )
            ), analog.normalGain
        )
    }
}