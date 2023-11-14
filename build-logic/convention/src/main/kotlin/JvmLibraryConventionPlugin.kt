import cn.wj.android.cashbook.buildlogic.ProjectSetting
import cn.wj.android.cashbook.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Java 仓库插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/13
 */
@Suppress("unused")
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ProjectSetting.Plugin.PLUGIN_KOTLIN_JVM)
            }
            configureKotlinJvm()
        }
    }
}
