package com.stream_suite.link.activity.main

import android.os.Build
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import com.stream_suite.link.activity.MessengerActivity
import com.stream_suite.link.shared.data.DataSource
import com.stream_suite.link.shared.service.jobs.ScheduledMessageJob
import com.stream_suite.link.shared.util.CursorUtil
import com.stream_suite.link.shared.util.NotificationUtils
import com.stream_suite.link.shared.util.show
import com.stream_suite.link.utils.TextAnywhereConversationCardApplier

class MainOnStartDelegate(private val activity: MessengerActivity) {

    private val navController
        get() = activity.navController

    fun run() {
        Handler().postDelayed({
            if (navController.conversationListFragment != null &&
                    !navController.conversationListFragment!!.isExpanded) {

                if (!navController.navigationView.isShown && navController.otherFragment == null) {
                    navController.navigationView.show()
                    activity.toolbar.alignTitleCenter()
                }
                showTextAnywherePromotion()
            }

            activity.snoozeController.updateSnoozeIcon()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                dismissAllActiveNotifications()
            }

            ScheduledMessageJob.scheduleNextRun(activity)
        }, 1000)
    }

    private fun showTextAnywherePromotion() {
        val convoList = navController.conversationListFragment!!
        val applier = TextAnywhereConversationCardApplier(convoList)
        val recycler = try {
            convoList.recyclerView
        } catch (e: Exception) {
            null
        }

        if (recycler != null && applier.shouldAddCardToList()) {
            val scrollToTop = recycler.layoutManager is LinearLayoutManager &&
                    (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0

            applier.addCardToConversationList()
            if (scrollToTop) {
                recycler.smoothScrollToPosition(0)
            }
        }
    }

    private fun dismissAllActiveNotifications() {
        Thread {
            val c = DataSource.getUnseenMessages(activity)
            val count = c.count
            CursorUtil.closeSilent(c)

            if (count > 1) {
                // since the notification functionality here is not nearly as good as 7.0,
                // we will just remove them all, if there is more than one
                try {
                    NotificationUtils.cancelAll(activity)
                } catch (e: IllegalStateException) {
                } catch (e: SecurityException) {
                }

            }
        }.start()
    }
}