/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Parallel Colt.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package com.example.healthwatchervitalsigns.vitals_checker_back_camera.math

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory

/**
 * Concurrency utilities.
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
object ConcurrencyUtils {
    /**
     * Thread pool.
     */
    private val THREAD_POOL =
        Executors.newCachedThreadPool(CustomThreadFactory(CustomExceptionHandler()))
    private var THREADS_BEGIN_N_1D_FFT_2THREADS = 8192
    private var THREADS_BEGIN_N_1D_FFT_4THREADS = 65536
    /**
     * Returns the minimal size of 2D data for which threads are used.
     *
     * @return the minimal size of 2D data for which threads are used
     */
    /**
     * Sets the minimal size of 2D data for which threads are used.
     *
     * @param n the minimal size of 2D data for which threads are used
     */
    var threadsBeginN_2D = 65536
    /**
     * Returns the minimal size of 3D data for which threads are used.
     *
     * @return the minimal size of 3D data for which threads are used
     */
    /**
     * Sets the minimal size of 3D data for which threads are used.
     *
     * @param n the minimal size of 3D data for which threads are used
     */
    var threadsBeginN_3D = 65536
    private var NTHREADS = prevPow2(numberOfProcessors)

    /**
     * Returns the number of available processors.
     *
     * @return number of available processors
     */
    val numberOfProcessors: Int
        get() = Runtime.getRuntime().availableProcessors()
    /**
     * Returns the current number of threads.
     *
     * @return the current number of threads.
     */
    /**
     * Sets the number of threads. If n is not a power-of-two number, then the
     * number of threads is set to the closest power-of-two number less than n.
     *
     * @param n
     */
    @JvmStatic
    var numberOfThreads: Int
        get() = NTHREADS
        set(n) {
            NTHREADS = prevPow2(n)
        }
    /**
     * Returns the minimal size of 1D data for which two threads are used.
     *
     * @return the minimal size of 1D data for which two threads are used
     */
    /**
     * Sets the minimal size of 1D data for which two threads are used.
     *
     * @param n the minimal size of 1D data for which two threads are used
     */
    @JvmStatic
    var threadsBeginN_1D_FFT_2Threads: Int
        get() = THREADS_BEGIN_N_1D_FFT_2THREADS
        set(n) {
            if (n < 512) {
                THREADS_BEGIN_N_1D_FFT_2THREADS = 512
            } else {
                THREADS_BEGIN_N_1D_FFT_2THREADS = n
            }
        }
    /**
     * Returns the minimal size of 1D data for which four threads are used.
     *
     * @return the minimal size of 1D data for which four threads are used
     */
    /**
     * Sets the minimal size of 1D data for which four threads are used.
     *
     * @param n the minimal size of 1D data for which four threads are used
     */
    @JvmStatic
    var threadsBeginN_1D_FFT_4Threads: Int
        get() = THREADS_BEGIN_N_1D_FFT_4THREADS
        set(n) {
            if (n < 512) {
                THREADS_BEGIN_N_1D_FFT_4THREADS = 512
            } else {
                THREADS_BEGIN_N_1D_FFT_4THREADS = n
            }
        }

    /**
     * Resets the minimal size of 1D data for which two and four threads are
     * used.
     */
    fun resetThreadsBeginN_FFT() {
        THREADS_BEGIN_N_1D_FFT_2THREADS = 8192
        THREADS_BEGIN_N_1D_FFT_4THREADS = 65536
    }

    /**
     * Resets the minimal size of 2D and 3D data for which threads are used.
     */
    fun resetThreadsBeginN() {
        threadsBeginN_2D = 65536
        threadsBeginN_3D = 65536
    }

    /**
     * Returns the closest power-of-two number greater than or equal to x.
     *
     * @param x
     * @return the closest power-of-two number greater than or equal to x
     */
    @JvmStatic
    fun nextPow2(x: Int): Int {
        var x = x
        require(x >= 1) { "x must be greater or equal 1" }
        if (x and x - 1 == 0) {
            return x // x is already a power-of-two number 
        }
        x = x or (x ushr 1)
        x = x or (x ushr 2)
        x = x or (x ushr 4)
        x = x or (x ushr 8)
        x = x or (x ushr 16)
        x = x or (x ushr 32)
        return x + 1
    }

    /**
     * Returns the closest power-of-two number less than or equal to x.
     *
     * @param x
     * @return the closest power-of-two number less then or equal to x
     */
    fun prevPow2(x: Int): Int {
        require(x >= 1) { "x must be greater or equal 1" }
        return Math.pow(
            2.0,
            Math.floor(Math.log(x.toDouble()) / Math.log(2.0))
        ).toInt()
    }

    /**
     * Checks if x is a power-of-two number.
     *
     * @param x
     * @return true if x is a power-of-two number
     */
    @JvmStatic
    fun isPowerOf2(x: Int): Boolean {
        return if (x <= 0) false else x and x - 1 == 0
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds.
     *
     * @param millis
     */
    fun sleep(millis: Long) {
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing
     * that task.
     *
     * @param task a Runnable task for execution
     * @return a Future representing the task
     */
    @JvmStatic
    fun submit(task: Runnable?): Future<*> {
        return THREAD_POOL.submit(task)
    }

    /**
     * Waits for all threads to complete computation.
     *
     * @param futures
     */
    @JvmStatic
    fun waitForCompletion(futures: Array<Future<*>>) {
        val size = futures.size
        try {
            for (j in 0 until size) {
                futures[j].get()
            }
        } catch (ex: ExecutionException) {
            ex.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private class CustomExceptionHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            e.printStackTrace()
        }
    }

    private class CustomThreadFactory internal constructor(private val handler: Thread.UncaughtExceptionHandler) :
        ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = defaultFactory.newThread(r)
            t.uncaughtExceptionHandler = handler
            return t
        }

        companion object {
            private val defaultFactory = Executors.defaultThreadFactory()
        }
    }
}