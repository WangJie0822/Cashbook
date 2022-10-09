@file:Suppress("unused", "UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.Project
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 维度枚举
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2022/9/2
 */
enum class FlavorDimension {
    ContentType
}

/**
 * 渠道枚举
 *
 * @param dimension 维度
 * @param signing 签名
 * @param applicationIdSuffix 应用 id 后缀
 * @param versionNameSuffix 版本名后缀
 * @param backupDirSuffix 备份文件夹后缀
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2022/9/2
 */
enum class Flavor(
    val dimension: FlavorDimension,
    val signing: Signing,
    val applicationIdSuffix: String?,
    val versionNameSuffix: String?,
    val backupDirSuffix: String
) {
    /** 正式渠道 */
    Online(FlavorDimension.ContentType, Signing.Android, null, "_online", "online"),

    /** 开发渠道 */
    Dev(FlavorDimension.ContentType, Signing.Android, ".dev", "_dev", "dev")
}

/**
 * 配置多渠道
 *
 * - Application 使用，配置多渠道并生成渠道枚举类
 */
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

                    signingConfig = signingConfigs.findByName(it.signing.name)

                    buildConfigField("String", "BACKUP_DIR_SUFFIX", "\"${it.backupDirSuffix}\"")
                }
            }
        }

        applicationVariants.all {
            generateBuildConfigProvider.get().let {
                it.doLast {
                    println("> Task :${project.name}:afterGenerateBuildConfig")
                    // 将枚举类生成到 BuildConfig 路径下
                    val enumPath = it.sourceOutputDir.asFile.get().path
                    val buildPkg = "${it.namespace.get()}.buildlogic"
                    println("> Task :${project.name}:generateFlavorEnumSource package: $buildPkg enumPath: $enumPath")
                    generateFlavor(buildPkg, enumPath)
                }
            }
        }
    }
}

/**
 * 配置多渠道
 *
 * - Library 使用，仅生成多渠道枚举类
 */
fun Project.configureFlavors(
    commonExtension: LibraryExtension
) {
    commonExtension.apply {
        libraryVariants.all {
            generateBuildConfigProvider.get().let {
                it.doLast {
                    println("> Task :${project.name}:afterGenerateBuildConfig")
                    // 将枚举类生成到 BuildConfig 路径下
                    val enumPath = it.sourceOutputDir.asFile.get().path
                    val buildPkg = "${it.namespace.get()}.buildlogic"
                    println("> Task :${project.name}:generateFlavorEnumSource package: $buildPkg enumPath: $enumPath")
                    generateFlavor(buildPkg, enumPath)
                }
            }
        }
    }
}

/**
 * 配置编译类型
 *
 * - Application 使用，debug 指定使用 release 签名
 */
fun Project.configureBuildTypes(
    commonExtension: BaseAppModuleExtension,
) {
    commonExtension.apply {
        buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = getByName("release").signingConfig
            }
            getByName("release") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
    }
}

/**
 * 配置编译类型
 *
 * - Library 使用
 */
fun Project.configureBuildTypes(
    commonExtension: LibraryExtension,
) {
    commonExtension.apply {
        buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            getByName("release") {
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
    }
}

/** 将多渠道枚举类生成到指定路径 [path] [pkg] 包下 */
private fun generateFlavor(pkg: String, path: String) {
    val flavor = TypeSpec.enumBuilder(Flavor::class.java.simpleName).apply {
        addModifiers(Modifier.PUBLIC)
        Flavor.values().forEach {
            addEnumConstant(it.name)
        }
    }.build()
    JavaFile.builder(pkg, flavor).build().writeTo(File(path))
}