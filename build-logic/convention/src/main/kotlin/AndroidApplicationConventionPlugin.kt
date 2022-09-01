import cn.wj.android.cashbook.buildlogic.configureFlavors
import cn.wj.android.cashbook.buildlogic.configureKotlinAndroid
import cn.wj.android.cashbook.buildlogic.configureSigning
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<BaseAppModuleExtension> {
                configureKotlinAndroid(this)
                configureSigning(this)
                configureFlavors(this)
            }
        }
    }

}