package com.francis.testuserswitching

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private var mAdminComponentName: ComponentName? = null
    private var mDevicePolicyManager: DevicePolicyManager? = null
    private lateinit var enableButton: Button
    private lateinit var disableButton: Button
    private lateinit var enableButtonWUS: Button


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdminComponentName = ComponentName(this, AppAdminReceiver::class.java)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        findViewById<TextView>(R.id.text).apply {
            text = if (!mDevicePolicyManager!!.isAdminActive(mAdminComponentName!!)) {
                "Not a device Admin"
            } else {
                "Is device Admin"
            }
        }

        enableButton = findViewById(R.id.enable_kiosk)
        enableButton.setOnClickListener(::onClickEnable)
        disableButton = findViewById(R.id.disable_kiosk)
        disableButton.setOnClickListener(::onClickDisable)
        enableButtonWUS = findViewById(R.id.enable_kiosk_us)
        enableButtonWUS.setOnClickListener(::onClickEnableWUS)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_device_owner -> {
                if (mDevicePolicyManager!!.isDeviceOwnerApp(packageName)) {
                    mDevicePolicyManager!!.clearDeviceOwnerApp("com.francis.testuserswitching")
                }
            }
            R.id.action_setting -> {
                //Lunch the settings
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage("com.android.settings")
                launchIntent?.let {
                    startActivity(it)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun onClickEnable(view: View) {
        // Consider locking your app here or by some other mechanism
        // Active Manager is supported on Android M
        // Consider locking your app here or by some other mechanism
        // Active Manager is supported on Android M
        if (mDevicePolicyManager!!.isDeviceOwnerApp(packageName)) {
            val  am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                setDefaultCosuPolicies(active = true, switchFlag = true)
                startLockTask()
                enableButtonWUS.isEnabled = false
            }
        } else Toast.makeText(this, "Please make application device owner", Toast.LENGTH_LONG).show()

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun onClickEnableWUS(view: View) {
        // Consider locking your app here or by some other mechanism
        // Active Manager is supported on Android M
        // Consider locking your app here or by some other mechanism
        // Active Manager is supported on Android M
        if (mDevicePolicyManager!!.isDeviceOwnerApp(packageName)) {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                setDefaultCosuPolicies(true)
                startLockTask()
                enableButton.isEnabled = false
            }
        } else Toast.makeText(this, "Please make application device owner", Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun onClickDisable(view: View) {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED) {
            stopLockTask()
            setDefaultCosuPolicies(false)
            enableButton.isEnabled = true
            enableButtonWUS.isEnabled = true
        } else {

            Toast.makeText(this, "Kiosk mode was not enabled.", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setDefaultCosuPolicies(active: Boolean, switchFlag: Boolean = false) {

        // Set user restrictions
        // Explicitly disallow logging out using Android UI (disabled by default).
        mDevicePolicyManager!!.setLogoutEnabled(mAdminComponentName!!, !switchFlag)

        // Disallow switching users in Android's UI. This DPC can still
        // call switchUser() to manage users.
        setUserRestriction(UserManager.DISALLOW_USER_SWITCH, switchFlag)

        setUserRestriction(UserManager.DISALLOW_ADD_USER, switchFlag)

        // Disable keyguard and status bar
        mDevicePolicyManager!!.setKeyguardDisabled(mAdminComponentName!!, active)
        mDevicePolicyManager!!.setStatusBarDisabled(mAdminComponentName!!, active)

        // Set system update policy
        if (active) {
            mDevicePolicyManager!!.setSystemUpdatePolicy(
                mAdminComponentName!!,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            mDevicePolicyManager!!.setSystemUpdatePolicy(mAdminComponentName!!, null)
        }

        // set this Activity as a lock task package
        mDevicePolicyManager!!.setLockTaskPackages(
            mAdminComponentName!!, if (active) arrayOf(
                packageName,
                "com.android.settings"
            ) else arrayOf()
        )
        val intentFilter = IntentFilter(Intent.ACTION_MAIN)
        intentFilter.addCategory(Intent.CATEGORY_HOME)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager!!.addPersistentPreferredActivity(
                mAdminComponentName!!, intentFilter, ComponentName(
                    packageName,
                    MainActivity::class.java.name
                )
            )
        } else {
            mDevicePolicyManager!!.clearPackagePersistentPreferredActivities(
                mAdminComponentName!!,
                packageName
            )
        }
    }

    //Set device policies
    private fun setUserRestriction(restriction: String, disallow: Boolean) {
        if (disallow) {
            mDevicePolicyManager!!.addUserRestriction(mAdminComponentName!!, restriction)
        } else {
            mDevicePolicyManager!!.clearUserRestriction(mAdminComponentName!!, restriction)
        }
    }
}