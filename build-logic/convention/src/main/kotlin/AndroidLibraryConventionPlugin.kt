import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import cn.wj.android.cashbook.buildlogic.configureBuildTypes
import cn.wj.android.cashbook.buildlogic.configureFlavors
import cn.wj.android.cashbook.buildlogic.configureKotlinAndroid
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android Kotlin Library 插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/7
 */
@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_ANDROID_LIBRARY)
                apply(ApplicationSetting.Plugin.PLUGIN_KOTLIN_ANDROID)
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureFlavors(this)
                configureBuildTypes(this)
            }
        }
    }
}
