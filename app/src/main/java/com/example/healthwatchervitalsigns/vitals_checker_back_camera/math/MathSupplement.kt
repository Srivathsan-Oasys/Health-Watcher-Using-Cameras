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
 * Useful math functions which come back over and over again
 */
object MathSupplement {
    var doubleLn10 = 2.3025850929940456840179914546844
    fun solve_quadratic_1(a: Double, b: Double, c: Double): Complex {
        return Complex(-b).add(Complex(b * b - 4 * a * c, 0.0)).sqrt()
            .divide(2.0 * a)
    }

    fun solve_quadratic_2(a: Double, b: Double, c: Double): Complex {
        return Complex(-b).subtract(Complex(b * b - 4 * a * c, 0.0))
            .sqrt().divide(2.0 * a)
    }

    fun adjust_imag(c: Complex): Complex {
        return if (Math.abs(c.imaginary) < 1e-30) Complex(c.real, 0.0) else c
    }

    fun addmul(c: Complex, v: Double, c1: Complex): Complex {
        return Complex(
            c.real + v * c1.real, c.imaginary + v
                    * c1.imaginary
        )
    }

    fun recip(c: Complex): Complex {
        val n = 1.0 / (c.abs() * c.abs())
        return Complex(n * c.real, n * c.imaginary)
    }

    fun asinh(x: Double): Double {
        return Math.log(x + Math.sqrt(x * x + 1))
    }

    fun acosh(x: Double): Double {
        return Math.log(x + Math.sqrt(x * x - 1))
    }
}