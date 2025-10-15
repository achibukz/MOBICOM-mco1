package com.mobdeve.s18.mco.utils

import android.content.Context
import android.content.Intent
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.activities.Activity_AddEntry
import com.mobdeve.s18.mco.activities.Activity_Home
import com.mobdeve.s18.mco.activities.Activity_Journal
import com.mobdeve.s18.mco.activities.Activity_Map

class BottomNavHelper {

    companion object {
        fun navigateToTab(context: Context, itemId: Int) {
            val intent = when (itemId) {
                R.id.nav_home -> Intent(context, Activity_Home::class.java)
                R.id.nav_map -> Intent(context, Activity_Map::class.java)
                R.id.nav_journal -> Intent(context, Activity_Journal::class.java)
                R.id.nav_add -> Intent(context, Activity_AddEntry::class.java)
                else -> return
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(intent)
        }

        fun getSelectedItemId(activityClass: Class<*>): Int {
            return when (activityClass) {
                Activity_Home::class.java -> R.id.nav_home
                Activity_Map::class.java -> R.id.nav_map
                Activity_Journal::class.java -> R.id.nav_journal
                Activity_AddEntry::class.java -> R.id.nav_add
                else -> R.id.nav_home
            }
        }
    }
}
