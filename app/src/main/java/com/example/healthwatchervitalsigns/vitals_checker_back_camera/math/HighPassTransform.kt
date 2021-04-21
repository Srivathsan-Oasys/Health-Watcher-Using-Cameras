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
 * Transforms from an analogue lowpass filter to a digital highpass filter
 */
class HighPassTransform(fc: Double, digital: LayoutBase, analog: LayoutBase) {
    var f: Double
    private fun transform(c: Complex): Complex {
        var c = c
        if (c.isInfinite) return Complex(1.0, 0.0)

        // frequency transform
        c = c.multiply(f)

        // bilinear high pass transform
        return Complex(-1.0).multiply(Complex(1.0).add(c)).divide(
            Complex(1.0).subtract(c)
        )
    }

    init {
        digital.reset()

        // prewarp
        f = 1.0 / Math.tan(Math.PI * fc)
        val numPoles = analog.numPoles
        val pairs = numPoles / 2
        for (i in 0 until pairs) {
            val pair = analog.getPair(i)
            digital.addPoleZeroConjugatePairs(
                transform(pair!!.poles!!.first),
                transform(pair.zeros!!.first)
            )
        }
        if (numPoles and 1 == 1) {
            val pair = analog.getPair(pairs)
            digital.add(
                transform(pair!!.poles!!.first),
                transform(pair.zeros!!.first)
            )
        }
        digital.setNormal(Math.PI - analog.normalW, analog.normalGain)
    }
}