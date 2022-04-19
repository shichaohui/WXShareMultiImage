package com.sch.share.utils

import java.util.*

/**
 * 简易定时器
 */
object EasyTimer {

    /**
     * 执行任务
     * @param duration 定时时间，单位 ms
     * @param task 任务
     */
    fun schedule(duration: Long, task: () -> Unit): Timer {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                task()
            }
        }, duration)
        return timer
    }
}