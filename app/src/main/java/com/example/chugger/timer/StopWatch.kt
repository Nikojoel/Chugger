package com.example.chugger.timer

/**
 * @author Nikojoel
 * StopWatch
 * Class that has functionality for a simple 60 second stopwatch
 */
class Stopwatch {

    // Variables
    private var startTime: Long = 0
    private var running = false
    private var currentTime: Int = 0

    /**
     * Start the stopwatch
     */
    fun start() {
        startTime = System.currentTimeMillis()
        running = true
    }

    /**
     * Stop the stopwatch
     */
    fun stop() {
        running = false
        currentTime = (System.currentTimeMillis() - startTime).toInt()
    }

    /**
     * Return the elapsed time in milliseconds
     * @return String
     */
    fun elapsedMill(): String {
        var elapsed = ""
            if (running) {
                // Get time as milliseconds and remove the last digit
                val time = "${((System.currentTimeMillis() - startTime) / 100) % 1000}"
                elapsed = time.substring(time.length - 1)
            }
            return elapsed
        }

    /**
     * Return the elapsed time in seconds
     * @return String
     */
    fun elapsedSec(): String {
        var elapsed = ""
        if (running) {
            val time = "${((System.currentTimeMillis() - startTime) / 1000) % 60}"

            // Set the time in xx:xx format if 10 seconds has passed
            if (time.length < 2) elapsed = "0$time:" else elapsed = "$time:"

            // Stop the stopwatch at 59 seconds
            if (time.contains("59")) {
                stop()
            }
        }
        return elapsed
    }

    /**
     * Returns the elapsed time in milliseconds
     * @return Int
     */
    fun getTotal(): Int {
        return currentTime
    }
}