@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.Project
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 维度枚举类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
enum class FlavorDimension {
    ContentType
}

/**
 * 渠道枚举类
 *
 * - 包含属性 [dimension] 维度，[applicationIdSuffix] 应用id后缀，[versionNameSuffix] 版本名后缀
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/1
 */
enum class Flavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val versionNameSuffix: String? = null
) {
    /** 正式渠道 */
    Online(FlavorDimension.ContentType, null, "_online"),

    /** 开发渠道 */
    Dev(FlavorDimension.ContentType, ".dev", "_dev")
}

/**
 * 配置多渠道
 */
@Suppress("unused")
fun Project.configureFlavors(
    commonExtension: BaseAppModuleExtension
) {
    commonExtension.apply {
        // 配置维度
        flavorDimensions += FlavorDimension.values().map { it.name }

        // 多渠道配置
        productFlavors {
            Flavor.values().forEach {
                create(it.name) {
                    dimension = it.dimension.name

                    it.applicationIdSuffix?.let { suffix ->
                        applicationIdSuffix = suffix
                    }

                    it.versionNameSuffix?.let { suffix ->
                        versionNameSuffix = suffix
                    }

                    buildConfigField("int", "BACKUP_VERSION", "1")
                }
            }
        }

        // 配置不同渠道源码路径
        sourceSets {
            getByName(Flavor.Online.name) {
                res.srcDirs("src/main/res-online")
            }
            getByName(Flavor.Dev.name) {
                res.srcDirs("src/main/res-dev")
            }
        }

        // 枚举类生成路径
        val sep = org.jetbrains.kotlin.konan.file.File.Companion.separator
        val enumPath = "${buildDir}${sep}generated${sep}source${sep}buildConfig"

        // 生成多渠道枚举类
        applicationVariants.all {
            javaCompileProvider.get().doLast {
                val flavor = Flavor.valueOf(flavorName)
                val appId = applicationId.removeSuffixIfPresent(flavor.applicationIdSuffix.orEmpty())
                val buildPkg = "${appId}.buildlogic"
                val path = "${enumPath}${sep}${flavor.name}${sep}${buildType.name}"
                println("> Task :build-logic:generateFlavorEnumSource package: $buildPkg path: $path")
                generateFlavor(buildPkg, path)
            }
        }
    }
}

/** 将多渠道枚举类生成到指定路径 [path] 下 */
fun generateFlavor(pkg: String, path: String) {
    val flavor = TypeSpec.enumBuilder(Flavor::class.java.simpleName).apply {
        addModifiers(Modifier.PUBLIC)
        Flavor.values().forEach {
            addEnumConstant(it.name)
        }
    }.build()
    JavaFile.builder(pkg, flavor).build().writeTo(File(path))
}