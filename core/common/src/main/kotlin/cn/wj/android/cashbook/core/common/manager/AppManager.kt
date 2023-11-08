/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.common.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import cn.wj.android.cashbook.core.common.ext.logger
import java.lang.ref.WeakReference
import java.util.Stack

/**
 * 应用程序 [Activity] 管理类
 * - 用于 [Activity] 管理和应用程序退出
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/3/16
 */
@Suppress("unused")
object AppManager {

    /** [Application] 对象 */
    private var mApplication: Application? = null

    /** 保存 [Activity] 对象的堆 */
    private val activityStack: Stack<WeakReference<Activity>> = Stack()

    /**
     * 忽略列表
     * > 列表中的 [Activity] 对象不会被纳入管理
     */
    private val ignoreActivities = arrayListOf<Class<out Activity>>()

    /** 前台界面数量 */
    private var foregroundCount = 0

    /** App 前后台切换回调 */
    private val mAppForegroundStatusChangeCallbacks = arrayListOf<ForegroundCallback>()

    /** Activity 生命周期回调接口*/
    private val mActivityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            onCreate(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // Empty block
        }

        override fun onActivityStarted(activity: Activity) {
            if (foregroundCount == 0) {
                // App 回到前台
                mAppForegroundStatusChangeCallbacks.forEach {
                    it.invoke(true)
                }
            }
            foregroundCount++
        }

        override fun onActivityResumed(activity: Activity) {
            // Empty block
        }

        override fun onActivityPaused(activity: Activity) {
            // Empty block
        }

        override fun onActivityStopped(activity: Activity) {
            foregroundCount--
            if (foregroundCount == 0) {
                // App 退到后台
                mAppForegroundStatusChangeCallbacks.forEach {
                    it.invoke(false)
                }
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            onDestroy(activity)
        }
    }

    /** 获取前台界面数量 */
    @JvmStatic
    fun getForegroundCount(): Int {
        return foregroundCount
    }

    /** 判断应用是否在前台 */
    @JvmStatic
    fun isForeground(): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName
        // 获取Android设备中所有正在运行的App
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName == packageName && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true
            }
        }
        return false
    }

    /**
     * 将 [AppManager] 注册到 [application]
     * > 应用启动后自动调用
     */
    fun register(application: Application) {
        mApplication = application
        application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }

    /**
     * 添加 App 前后台状态切换监听[onChange]
     * > [onChange] 入参[Boolean] `true`回到前台 & `false`进入后台
     */
    @JvmStatic
    fun addOnAppForegroundStatusChangeListener(onChange: ForegroundCallback) {
        mAppForegroundStatusChangeCallbacks.add(onChange)
    }

    /**
     * 移除 App 前后台状态切换监听[onChange]
     * > [onChange] 入参[Boolean] `true`回到前台 & `false`进入后台
     */
    @JvmStatic
    fun removeOnAppForegroundStatusChangeListener(onChange: ForegroundCallback) {
        mAppForegroundStatusChangeCallbacks.remove(onChange)
    }

    /**
     * 获取可用的 [Application] 对象
     * > [AppManager]已初始化，返回[mApplication]，否则通过反射[getApplicationByReflect]
     * > 获取当前[Application]对象，若获取失败，抛出异常[NullPointerException]
     */
    @JvmStatic
    @Throws(NullPointerException::class)
    fun getApplication(): Application {
        if (mApplication == null) {
            register(getApplicationByReflect())
            if (mApplication == null) {
                throw NullPointerException("Application must not be null! Please register AppManager in your Application start！")
            }
        }
        return mApplication!!
    }

    @JvmStatic
    val context: Context
        get() = peekActivity() ?: getApplication()

    @JvmStatic
    val applicationContext: Context
        get() = context.applicationContext

    /**
     * 通过反射获取当前 [Application] 对象
     * > 获取失败时抛出[NullPointerException]
     */
    @Throws(NullPointerException::class)
    private fun getApplicationByReflect(): Application {
        try {
            @SuppressLint("PrivateApi")
            val activityThread = Class.forName("android.app.ActivityThread")
            val thread = activityThread.getMethod("currentActivityThread").invoke(null)
            val app = activityThread.getMethod("getApplication").invoke(thread)
                ?: throw NullPointerException("u should init first")
            return app as Application
        } catch (e: Exception) {
            logger().e(e, "getApplicationByReflect")
        }
        throw NullPointerException("u should init first")
    }

    /**
     * 将[Activity]类对象列表[classArray]添加到忽略列表[ignoreActivities]
     * > 忽略列表[ignoreActivities]中的[Activity]类不会被[AppManager]管理
     */
    @JvmStatic
    fun addToIgnore(vararg classArray: Class<out Activity>) {
        ignoreActivities.addAll(classArray)
    }

    /** [Activity.onCreate] 时回调，将不在忽略列表[ignoreActivities]中的[activity]添加到管理堆[activityStack] */
    @JvmStatic
    private fun onCreate(activity: Activity?) {
        if (activity != null && ignoreActivities.contains(activity.javaClass)) {
            // 不管理在忽略列表中的 Activity
            return
        }
        add(activity)
    }

    /** [Activity.onDestroy] 时回调，将[activity]从管理堆[activityStack]中移除 */
    @JvmStatic
    private fun onDestroy(activity: Activity?) {
        remove(activity)
    }

    /** 判断当前堆[activityStack]中是否存在[clazz]对应的[Activity] */
    @JvmStatic
    fun contains(clazz: Class<out Activity>): Boolean {
        return activityStack.count { it.get()?.javaClass == clazz } > 0
    }

    /** 将[activity]添加到[activityStack] */
    @JvmStatic
    fun add(activity: Activity?) {
        if (activity == null) {
            return
        }
        activityStack.add(WeakReference(activity))
    }

    /** 将[activity]从[activityStack]中移除 */
    @JvmStatic
    fun remove(activity: Activity?) {
        if (activity == null) {
            return
        }
        val index = activityStack.indexOfFirst {
            it.get() == activity
        }
        if (index in 0 until activityStack.size) {
            activityStack.removeElementAt(index)
        }
    }

    /** 关闭[activityStack]中，除了[activities]以外的[Activity] */
    @JvmStatic
    fun finishAllWithout(vararg activities: Activity?) {
        if (activities.isEmpty()) {
            return
        }
        activities.forEach {
            remove(it)
        }
        finishAllActivity()
        activities.forEach {
            add(it)
        }
    }

    /** 关闭[activityStack]中，除了类型在[classArray]以外的[Activity] */
    @JvmStatic
    fun finishAllWithout(vararg classArray: Class<out Activity>) {
        if (classArray.isEmpty()) {
            return
        }
        val ls = arrayListOf<Activity>()
        classArray.forEach {
            val elements = getActivities(it)
            ls.addAll(elements)
        }
        ls.forEach {
            remove(it)
        }
        finishAllActivity()
        for (activity in ls) {
            add(activity)
        }
    }

    /** 关闭类对象为[clazz]的[Activity] */
    @JvmStatic
    fun finishActivity(clazz: Class<out Activity>) {
        getActivities(clazz).forEach { activity ->
            activity.finish()
        }
    }

    /** 关闭类型为[A]的[Activity] */
    inline fun <reified A : Activity> finishActivity() {
        finishActivity(A::class.java)
    }

    /** 关闭类型在[classArray]的[Activity] */
    @JvmStatic
    fun finishActivities(vararg classArray: Class<out Activity>) {
        for (clazz in classArray) {
            finishActivity(clazz)
        }
    }

    /** 获取[activityStack]堆顶的[Activity] */
    @JvmStatic
    fun peekActivity(): Activity? {
        return if (activityStack.isEmpty()) {
            null
        } else {
            activityStack.peek().get()
        }
    }

    /** 获取类对象为[clazz]的[Activity]列表[List] */
    @JvmStatic
    fun <A : Activity> getActivities(clazz: Class<out A>): List<A> {
        return activityStack.filter {
            it.get()?.javaClass == clazz
        }.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            it.get() as? A
        }
    }

    /** 获取类型为[A]的[Activity]列表[List] */
    inline fun <reified A : Activity> getActivities(): List<A> {
        return getActivities(A::class.java)
    }

    /** 获取堆[activityStack]中[Activity]的数量 */
    @JvmStatic
    fun getStackSize(): Int {
        return activityStack.size
    }

    /** 关闭[activityStack]中的所有[Activity] */
    @JvmStatic
    fun finishAllActivity() {
        activityStack.forEach {
            it.get()?.finish()
        }
        activityStack.clear()
    }
}

typealias ForegroundCallback = (Boolean) -> Unit
