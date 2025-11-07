package cn.nepuko.sklhelper.util

import kotlin.random.Random

object SklTicketGenerator {
    /**
     * Generate a random skl-ticket for API requests.
     * Based on Go/Python implementation: generates a 21-character random string
     * using a specific character set.
     */
    fun generate(): String {
        val charset = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict"
        val length = 21
        return (1..length)
            .map { charset[Random.nextInt(charset.length)] }
            .joinToString("")
    }
}

