package com.example.chugger.timer

class Stopwatch {

    private var startTime: Long = 0
    private var running = false
    private var currentTime: Int = 0

    fun start() {
        startTime = System.currentTimeMillis()
        running = true
    }

    fun stop() {
        running = false
        currentTime = (System.currentTimeMillis() - startTime).toInt()
    }

    fun elapsedMill(): String {
        var elapsed = ""
            if (running) {
                val time = "${((System.currentTimeMillis() - startTime) / 100) % 1000}"
                elapsed = time.substring(time.length - 1)
            }
            return elapsed
        }

    fun elapsedSec(): String {
        var elapsed = ""
        if (running) {
            val time = "${((System.currentTimeMillis() - startTime) / 1000) % 60}"
            if (time.length < 2) elapsed = "0$time:" else elapsed = "$time:"

            if (time.contains("59")) {
                stop()
            }
        }
        return elapsed
    }

    fun getTotal(): Int {
        return currentTime
    }
}