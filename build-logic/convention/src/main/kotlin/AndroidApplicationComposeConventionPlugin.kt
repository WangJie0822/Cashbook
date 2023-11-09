import cn.wj.android.cashbook.buildlogic.ProjectSetting
import cn.wj.android.cashbook.buildlogic.configureAndroidCompose
import cn.wj.android.cashbook.buildlogic.version
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Kotlin Android Compose Application 插件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
@Suppress("unused")
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinProjectExtension> {
                jvmToolchain(ProjectSetting.Config.javaVersion.version)
            }

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }

}