/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stream_suite.link.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.stream_suite.link.R
import com.stream_suite.link.activity.compose.ComposeActivity
import com.stream_suite.link.activity.main.*
import com.stream_suite.link.fragment.PrivateConversationListFragment
import com.stream_suite.link.fragment.settings.MyAccountFragment
import com.stream_suite.link.shared.data.Settings
import com.stream_suite.link.shared.service.notification.NotificationConstants
import com.stream_suite.link.shared.util.AnimationUtils
import com.stream_suite.link.shared.util.PromotionUtils
import com.stream_suite.link.shared.util.UnreadBadger
import com.stream_suite.link.shared.util.UpdateUtils
import com.stream_suite.link.shared.view.WhitableToolbar
import com.stream_suite.link.shared.widget.MessengerAppWidgetProvider


/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
class MessengerActivity : AppCompatActivity() {

    val navController = MainNavigationController(this)
    val accountController = MainAccountController(this)
    val intentHandler = MainIntentHandler(this)
    val searchHelper = MainSearchHelper(this)
    val snoozeController = SnoozeController(this)
    val insetController = MainInsetController(this)
    private val colorController = MainColorController(this)
    private val startDelegate = MainOnStartDelegate(this)
    private val permissionHelper = MainPermissionHelper(this)
    private val resultHandler = MainResultHandler(this)

    val toolbar: WhitableToolbar by lazy { findViewById<View>(R.id.toolbar) as WhitableToolbar }
    val snackbarContainer: FrameLayout by lazy { findViewById<FrameLayout>(R.id.snackbar_container) }
    private val content: View by lazy { findViewById<View>(android.R.id.content) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UpdateUtils(this).checkForUpdate()

        setContentView(R.layout.activity_main)

        initToolbar()

        colorController.configureGlobalColors()
        colorController.configureNavigationBarColor()
        intentHandler.dismissIfFromNotification()
        intentHandler.restoreNavigationSelection(savedInstanceState)
        navController.conversationActionDelegate.displayConversations(savedInstanceState)
        intentHandler.displayPrivateFromNotification()
        navController.initToolbarTitleClick()
        accountController.startIntroOrLogin(savedInstanceState)
        permissionHelper.requestDefaultSmsApp()
        insetController.applyWindowStatusFlags()
        insetController.overrideInsetsForStatusBar()

        val content = findViewById<View>(R.id.content)
        content.post {
            AnimationUtils.conversationListSize = content.height
            AnimationUtils.toolbarSize = toolbar.height
        }
    }

    public override fun onStart() {
        super.onStart()

        PromotionUtils(this).checkPromotions {
            MyAccountFragment.openTrialUpgradePreference = true
            clickNavigationItem(R.id.drawer_account)
        }

        UnreadBadger(this).clearCount()

        colorController.colorActivity()
        navController.initDrawer()
        intentHandler.displayAccount()
        intentHandler.handleShortcutIntent(intent)
        accountController.listenForFullRefreshes()
        accountController.refreshAccountToken()

        startDelegate.run()

        try {
            if (navController.otherFragment is PrivateConversationListFragment) {
                if (navController.isOtherFragmentConvoAndShowing()) {
                    navController.getShownConversationList()?.expandedItem?.itemView?.performClick()
                }

                clickNavigationItem(R.id.navigation_inbox)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentHandler.newIntent(intent)
    }

    public override fun onResume() {
        super.onResume()
        insetController.onResume()

        val messageListFragment = navController.findMessageListFragment()
        if (messageListFragment != null) {
            NotificationConstants.CONVERSATION_ID_OPEN = messageListFragment.conversationId
        }
    }

    public override fun onPause() {
        super.onPause()

        if (navController.conversationListFragment != null) {
            navController.conversationListFragment!!.swipeHelper.dismissSnackbars()
        }

        insetController.onPause()
        NotificationConstants.CONVERSATION_ID_OPEN = 0L
    }

    public override fun onStop() {
        super.onStop()

        MessengerAppWidgetProvider.refreshWidget(this)
        accountController.stopListeningForRefreshes()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(content.windowToken, 0)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        var outState = intentHandler.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    public override fun onDestroy() {
        super.onDestroy()
        accountController.stopListeningForDownloads()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        try {
            if (searchHelper.closeSearch()) {
            } else if (!navController.backPressed()) {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        supportActionBar?.setDisplayHomeAsUpEnabled(navController.inSettings)
        if (!navController.inSettings) {
            menuInflater.inflate(R.menu.activity_messenger, menu)

            val searchItem = navController.navigationView.menu.findItem(R.id.navigation_search)
            searchHelper.setup(searchItem)
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_back)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return navController.onNavigationItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            Settings.forceUpdate(this)
            colorController.colorActivity()
            colorController.configureGlobalColors()

            resultHandler.handle(requestCode, resultCode, data)
            accountController.startLoad(requestCode)
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun clickNavigationItem(itemId: Int) { navController.onNavigationItemSelected(itemId) }
    fun displayConversations() = navController.conversationActionDelegate.displayConversations()

    fun composeMessage() = startActivity(Intent(applicationContext, ComposeActivity::class.java))

    private fun initToolbar() {
        setSupportActionBar(toolbar)
    }
}
