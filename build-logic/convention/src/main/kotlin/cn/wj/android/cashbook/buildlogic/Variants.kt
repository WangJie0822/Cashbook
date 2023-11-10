@file:Suppress("unused", "UnstableApiUsage", "UnusedReceiverParameter")

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.File
import javax.lang.model.element.Modifier
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * 维度枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/2
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
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/9/2
 */
enum class CashbookFlavor(
    val dimension: FlavorDimension,
    val signing: Signing,
    val applicationIdSuffix: String?,
    val versionNameSuffix: String?,
) {
    /** 在线渠道，需要网络请求 */
    Online(FlavorDimension.ContentType, Signing.Android, null, "_online"),

    /** 离线渠道，无网络请求 */
    Offline(FlavorDimension.ContentType, Signing.Android, null, "_offline"),

    /** 尝鲜渠道 */
    Canary(FlavorDimension.ContentType, Signing.Android, null, "_canary"),

    /** 开发渠道 */
    Dev(FlavorDimension.ContentType, Signing.Android, ".dev", "_dev")
}

/**
 * 配置多渠道
 *
 * - Application 使用，配置多渠道并生成渠道枚举类
 */
fun Project.configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: CashbookFlavor) -> Unit = {},
) {
    commonExtension.apply {

        if (this is BaseAppModuleExtension) {
            // Application，配置签名
            configureSigningConfigs(this)
        }

        // 配置维度
        flavorDimensions += FlavorDimension.values().map { it.name }
        println("> Task :${project.name}:configureFlavors set dimensions: $flavorDimensions")

        // 多渠道配置
        productFlavors {

            val signingLibs = runCatching {
                extensions.getByType<VersionCatalogsExtension>().named("signingLibs")
            }.getOrNull()

            CashbookFlavor.values().forEach {
                create(it.name) {
                    dimension = it.dimension.name
                    println("> Task :${project.name}:configureFlavors set flavors: ${it.name}")
                    flavorConfigurationBlock(this, it)
                    if (this@apply is BaseAppModuleExtension && this is ApplicationProductFlavor) {
                        it.applicationIdSuffix?.let { suffix ->
                            applicationIdSuffix = suffix
                        }

                        it.versionNameSuffix?.let { suffix ->
                            versionNameSuffix = suffix
                        }

                        signingConfig = if (null != signingLibs) {
                            signingConfigs.findByName(it.signing.name)
                        } else {
                            println("> Task :${project.name}:configureFlavors signing config not found, use debug signing")
                            signingConfigs.findByName("debug")
                        }
                    }
                }
            }
        }
    }
}

fun Project.configureGenerateFlavors(
    commonExtension: CommonExtension<*, *, *, *, *>,
) {
    commonExtension.apply {
        println("> Task :${project.name}:configureFlavors generateFlavorFile")
        when (this) {
            is BaseAppModuleExtension -> applicationVariants
            is LibraryExtension -> libraryVariants
            else -> null
        }?.all {
            generateBuildConfigProvider?.get()?.let {
                it.doLast {
                    println("> Task :${project.name}:afterGenerateBuildConfig generateFlavorFile")
                    // 将枚举类生成到 BuildConfig 路径下
                    val enumPath = it.sourceOutputDir.asFile.get().path
                    val buildPkg = "${it.namespace.get()}.buildlogic"
                    println("> Task :${project.name}:beforeGenerateBuildConfig:generateFlavor package-$buildPkg enumPath-$enumPath")
                    generateFlavor(buildPkg, enumPath)
                }
            }
        }
    }
}

/** 将多渠道枚举类生成到指定路径 [path] [pkg] 包下 */
private fun generateFlavor(pkg: String, path: String) {
    val flavor = TypeSpec.enumBuilder(CashbookFlavor::class.java.simpleName).apply {
        addModifiers(Modifier.PUBLIC)
        CashbookFlavor.values().forEach {
            addEnumConstant(it.name)
        }
    }.build()
    JavaFile.builder(pkg, flavor).build().writeTo(File(path))
}