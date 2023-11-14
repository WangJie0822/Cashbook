import cn.wj.android.cashbook.buildlogic.ProjectSetting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(ProjectSetting.Plugin.PLUGIN_GOOGLE_HILT)
                apply(ProjectSetting.Plugin.PLUGIN_GOOGLE_KSP)
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("implementation", libs.findLibrary("google.hilt.android").get())
                add("ksp", libs.findLibrary("google.hilt.compiler").get())
                add("kspAndroidTest", libs.findLibrary("google.hilt.compiler").get())
                add("kspTest", libs.findLibrary("google.hilt.compiler").get())
            }
        }
    }

}