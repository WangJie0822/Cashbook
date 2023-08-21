import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import cn.wj.android.cashbook.buildlogic.configureGradleManagedDevices
import cn.wj.android.cashbook.buildlogic.configureKotlinAndroid
import cn.wj.android.cashbook.buildlogic.configurePrintApksTask
import cn.wj.android.cashbook.buildlogic.version
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Kotlin Android Application 插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_ANDROID_APPLICATION)
                apply(ApplicationSetting.Plugin.PLUGIN_KOTLIN_ANDROID)
            }

            extensions.configure<KotlinProjectExtension> {
                jvmToolchain(ApplicationSetting.Config.javaVersion.version)
            }

            extensions.configure<ApplicationExtension> {

                configureKotlinAndroid(this)
                with(defaultConfig) {
                    targetSdk = ApplicationSetting.Config.targetSdk

                    // 应用版本号
                    versionCode = ApplicationSetting.Config.versionCode
                    // 应用版本名
                    versionName = ApplicationSetting.Config.versionName
                }
                configureGradleManagedDevices(this)
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                configurePrintApksTask(this)
            }
        }
    }

}