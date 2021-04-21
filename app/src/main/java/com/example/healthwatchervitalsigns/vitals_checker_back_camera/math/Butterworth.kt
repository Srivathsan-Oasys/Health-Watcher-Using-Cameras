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
import org.apache.commons.math3.complex.ComplexUtils

/**
 * User facing class which contains all the methods the user uses
 * to create Butterworth filters. This done in this way:
 * Butterworth butterworth = new Butterworth();
 * Then call one of the methods below to create
 * low-,high-,band-, or stopband filters. For example:
 * butterworth.bandPass(2,250,50,5);
 */
class Butterworth : Cascade() {
    internal inner class AnalogLowPass(private val nPoles: Int) : LayoutBase(
        nPoles
    ) {
        fun design() {
            reset()
            val n2 = (2 * nPoles).toDouble()
            val pairs = nPoles / 2
            for (i in 0 until pairs) {
                val c: Complex = ComplexUtils.polar2Complex(
                    1.0, Math.PI / 2.0
                            + (2 * i + 1) * Math.PI / n2
                )
                addPoleZeroConjugatePairs(c, Complex.INF)
            }
            if (nPoles and 1 == 1) add(Complex(-1.0), Complex.INF)
        }

        init {
            setNormal(0.0, 1.0)
        }
    }

    private fun setupLowPass(
        order: Int, sampleRate: Double,
        cutoffFrequency: Double, directFormType: Int
    ) {
        val m_analogProto = AnalogLowPass(order)
        m_analogProto.design()
        val m_digitalProto = LayoutBase(order)
        LowPassTransform(
            cutoffFrequency / sampleRate, m_digitalProto,
            m_analogProto
        )
        setLayout(m_digitalProto, directFormType)
    }

    /**
     * Butterworth Lowpass filter with default topology
     *
     * @param order           The order of the filter
     * @param sampleRate      The sampling rate of the system
     * @param cutoffFrequency the cutoff frequency
     */
    fun lowPass(order: Int, sampleRate: Double, cutoffFrequency: Double) {
        setupLowPass(
            order, sampleRate, cutoffFrequency,
            DirectFormAbstract.DIRECT_FORM_II
        )
    }

    /**
     * Butterworth Lowpass filter with custom topology
     *
     * @param order           The order of the filter
     * @param sampleRate      The sampling rate of the system
     * @param cutoffFrequency The cutoff frequency
     * @param directFormType  The filter topology. This is either
     * DirectFormAbstract.DIRECT_FORM_I or DIRECT_FORM_II
     */
    fun lowPass(
        order: Int, sampleRate: Double, cutoffFrequency: Double,
        directFormType: Int
    ) {
        setupLowPass(order, sampleRate, cutoffFrequency, directFormType)
    }

    private fun setupHighPass(
        order: Int, sampleRate: Double,
        cutoffFrequency: Double, directFormType: Int
    ) {
        val m_analogProto = AnalogLowPass(order)
        m_analogProto.design()
        val m_digitalProto = LayoutBase(order)
        HighPassTransform(
            cutoffFrequency / sampleRate, m_digitalProto,
            m_analogProto
        )
        setLayout(m_digitalProto, directFormType)
    }

    /**
     * Highpass filter with custom topology
     *
     * @param order           Filter order (ideally only even orders)
     * @param sampleRate      Sampling rate of the system
     * @param cutoffFrequency Cutoff of the system
     * @param directFormType  The filter topology. See DirectFormAbstract.
     */
    fun highPass(
        order: Int, sampleRate: Double, cutoffFrequency: Double,
        directFormType: Int
    ) {
        setupHighPass(order, sampleRate, cutoffFrequency, directFormType)
    }

    /**
     * Highpass filter with default filter topology
     *
     * @param order           Filter order (ideally only even orders)
     * @param sampleRate      Sampling rate of the system
     * @param cutoffFrequency Cutoff of the system
     */
    fun highPass(order: Int, sampleRate: Double, cutoffFrequency: Double) {
        setupHighPass(
            order, sampleRate, cutoffFrequency,
            DirectFormAbstract.DIRECT_FORM_II
        )
    }

    private fun setupBandStop(
        order: Int, sampleRate: Double,
        centerFrequency: Double, widthFrequency: Double, directFormType: Int
    ) {
        val m_analogProto = AnalogLowPass(order)
        m_analogProto.design()
        val m_digitalProto = LayoutBase(order * 2)
        BandStopTransform(
            centerFrequency / sampleRate, widthFrequency
                    / sampleRate, m_digitalProto, m_analogProto
        )
        setLayout(m_digitalProto, directFormType)
    }

    /**
     * Bandstop filter with default topology
     *
     * @param order           Filter order (actual order is twice)
     * @param sampleRate      Samping rate of the system
     * @param centerFrequency Center frequency
     * @param widthFrequency  Width of the notch
     */
    fun bandStop(
        order: Int, sampleRate: Double, centerFrequency: Double,
        widthFrequency: Double
    ) {
        setupBandStop(
            order, sampleRate, centerFrequency, widthFrequency,
            DirectFormAbstract.DIRECT_FORM_II
        )
    }

    /**
     * Bandstop filter with custom topology
     *
     * @param order           Filter order (actual order is twice)
     * @param sampleRate      Samping rate of the system
     * @param centerFrequency Center frequency
     * @param widthFrequency  Width of the notch
     * @param directFormType  The filter topology
     */
    fun bandStop(
        order: Int, sampleRate: Double, centerFrequency: Double,
        widthFrequency: Double, directFormType: Int
    ) {
        setupBandStop(
            order, sampleRate, centerFrequency, widthFrequency,
            directFormType
        )
    }

    private fun setupBandPass(
        order: Int, sampleRate: Double,
        centerFrequency: Double, widthFrequency: Double, directFormType: Int
    ) {
        val m_analogProto = AnalogLowPass(order)
        m_analogProto.design()
        val m_digitalProto = LayoutBase(order * 2)
        BandPassTransform(
            centerFrequency / sampleRate, widthFrequency
                    / sampleRate, m_digitalProto, m_analogProto
        )
        setLayout(m_digitalProto, directFormType)
    }

    /**
     * Bandpass filter with default topology
     *
     * @param order           Filter order
     * @param sampleRate      Sampling rate
     * @param centerFrequency Center frequency
     * @param widthFrequency  Width of the notch
     */
    fun bandPass(
        order: Int, sampleRate: Double, centerFrequency: Double,
        widthFrequency: Double
    ) {
        setupBandPass(
            order, sampleRate, centerFrequency, widthFrequency,
            DirectFormAbstract.DIRECT_FORM_II
        )
    }

    /**
     * Bandpass filter with custom topology
     *
     * @param order           Filter order
     * @param sampleRate      Sampling rate
     * @param centerFrequency Center frequency
     * @param widthFrequency  Width of the notch
     * @param directFormType  The filter topology (see DirectFormAbstract)
     */
    fun bandPass(
        order: Int, sampleRate: Double, centerFrequency: Double,
        widthFrequency: Double, directFormType: Int
    ) {
        setupBandPass(
            order, sampleRate, centerFrequency, widthFrequency,
            directFormType
        )
    }
}