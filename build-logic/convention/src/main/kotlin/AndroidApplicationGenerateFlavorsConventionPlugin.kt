import cn.wj.android.cashbook.buildlogic.configureGenerateFlavors
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 生成多渠道枚举配置插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/13
 */
@Suppress("unused")
class AndroidApplicationGenerateFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                configureGenerateFlavors(this)
            }
        }
    }
}