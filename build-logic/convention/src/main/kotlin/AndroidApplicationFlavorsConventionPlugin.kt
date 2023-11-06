import cn.wj.android.cashbook.buildlogic.configureFlavors
import cn.wj.android.cashbook.buildlogic.configureSigningConfigs
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 多渠道配置插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/13
 */
@Suppress("unused")
class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                configureSigningConfigs(this)
                configureFlavors(this)
            }
        }
    }
}