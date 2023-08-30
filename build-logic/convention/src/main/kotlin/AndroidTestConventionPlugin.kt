import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import cn.wj.android.cashbook.buildlogic.configureGradleManagedDevices
import cn.wj.android.cashbook.buildlogic.configureKotlinAndroid
import com.android.build.gradle.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 测试插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/13
 */
@Suppress("unused")
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_ANDROID_TEST)
                apply(ApplicationSetting.Plugin.PLUGIN_KOTLIN_ANDROID)
            }

            extensions.configure<TestExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = ApplicationSetting.Config.TARGET_SDK
                configureGradleManagedDevices(this)
            }
        }
    }

}