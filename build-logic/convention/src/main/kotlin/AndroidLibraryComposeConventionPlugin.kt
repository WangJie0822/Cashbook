import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import cn.wj.android.cashbook.buildlogic.configureAndroidCompose
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Kotlin Android Compose Library 插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
@Suppress("unused")
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_ANDROID_LIBRARY)
            }

            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)
        }
    }

}