import cn.wj.android.cashbook.buildlogic.ApplicationSetting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ApplicationSetting.Plugin.PLUGIN_GOOGLE_HILT)
                apply(ApplicationSetting.Plugin.PLUGIN_KOTLIN_KAPT)
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("implementation", libs.findLibrary("google.hilt.android").get())
                add("kapt", libs.findLibrary("google.hilt.compiler").get())
                add("kaptAndroidTest", libs.findLibrary("google.hilt.compiler").get())
            }
        }
    }

}