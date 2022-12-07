package com.cmk.call

interface IntentProvider<T> {

    fun provider(t: T)
}