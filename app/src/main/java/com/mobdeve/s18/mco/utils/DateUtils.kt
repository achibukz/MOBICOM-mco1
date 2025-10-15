package com.mobdeve.s18.mco.utils

import java.text.SimpleDateFormat
import java.util.*

class DateUtils {

    companion object {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        fun formatDate(timestamp: Long): String {
            return dateFormat.format(Date(timestamp))
        }

        fun formatTime(timestamp: Long): String {
            return timeFormat.format(Date(timestamp))
        }

        fun formatDateTime(timestamp: Long): String {
            return dateTimeFormat.format(Date(timestamp))
        }

        fun formatMonthYear(timestamp: Long): String {
            return monthYearFormat.format(Date(timestamp))
        }

        fun getCurrentTimestamp(): Long {
            return System.currentTimeMillis()
        }

        fun getStartOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getEndOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.timeInMillis
        }

        fun isSameMonth(timestamp1: Long, timestamp2: Long): Boolean {
            val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
            val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
        }
    }
}
