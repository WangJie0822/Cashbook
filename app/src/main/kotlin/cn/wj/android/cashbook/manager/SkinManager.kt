package cn.wj.android.cashbook.manager

import android.app.Application
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.third.skin.SkinMaterialViewInflaterCompat
import skin.support.SkinCompatManager
import skin.support.app.SkinAppCompatViewInflater
import skin.support.app.SkinCardViewInflater
import skin.support.constraint.app.SkinConstraintViewInflater
import skin.support.design.app.SkinMaterialViewInflater

/**
 * 皮肤管理类
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/4/26
 */
object SkinManager {

    /** 使用 [application] 对象对皮肤管理进行初始化 */
    fun init(application: Application) {
        logger().d("init $application")
        SkinCompatManager.withoutActivity(application)
            .addInflater(SkinAppCompatViewInflater())
            .addInflater(SkinMaterialViewInflater())
            .addInflater(SkinConstraintViewInflater())
            .addInflater(SkinCardViewInflater())
            .addInflater(SkinMaterialViewInflaterCompat())
            .loadSkin("", SkinCompatManager.SKIN_LOADER_STRATEGY_NONE)
    }

    /** 加载 [skin] 对应的皮肤 */
    fun loadSkin(skin: String) {
        logger().d("loadSkin skin: $skin")
    }
}