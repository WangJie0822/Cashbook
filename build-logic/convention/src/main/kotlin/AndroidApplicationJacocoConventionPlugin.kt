import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import cn.wj.android.cashbook.buildlogic.configureJacoco
import cn.wj.android.cashbook.buildlogic.version
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class AndroidApplicationJacocoConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_ANDROID_APPLICATION)
                apply(ApplicationSetting.Plugin.PLUGIN_JACOCO)
            }

            extensions.configure<KotlinProjectExtension> {
                jvmToolchain(ApplicationSetting.Config.javaVersion.version)
            }

            val extension = extensions.getByType<ApplicationAndroidComponentsExtension>()
            configureJacoco(extension)
        }
    }

}