package com.example.data.repository

expect fun getSyncDateRange(): Pair<String, String>

expect fun getCurrentTimeMillis(): Long
