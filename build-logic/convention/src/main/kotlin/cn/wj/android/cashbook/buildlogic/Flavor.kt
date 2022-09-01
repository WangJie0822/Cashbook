@file:Suppress("UnstableApiUsage")

package cn.wj.android.cashbook.buildlogic

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.gradle.api.Project
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

        // 生成多渠道枚举类
        applicationVariants.all {
            javaCompileProvider.get().doLast {
                val chars = buildType.name.toCharArray()
                chars.first().toUpperCase()
                val buildTypeName = chars.concatToString()
                val path = "$buildDir/generated/source/kapt/$flavorName$buildTypeName"
                println("> :generateFlavorSource path $path")
                generateFlavor(path)
            }
        }
    }
}

/** 将多渠道枚举类生成到指定路径 [path] 下 */
fun generateFlavor(path: String) {
    val pkg = "cn.wj.android.cashbook.buildlogic"

    val flavorDimension = TypeSpec.enumBuilder(FlavorDimension::class.simpleName).apply {
        addModifiers(Modifier.PUBLIC)
        FlavorDimension.values().forEach {
            addEnumConstant(it.name)
        }
    }.build()

    JavaFile.builder(pkg, flavorDimension).build().writeTo(File(path))

    val flavor = TypeSpec.enumBuilder(Flavor::class.java.simpleName).apply {
        addModifiers(Modifier.PUBLIC)

        val parameters = Flavor::class.java.declaredConstructors.firstOrNull()?.parameters?.drop(2)

        parameters?.forEach {
            addField(it.type, it.name)
        }

        addMethod(MethodSpec.constructorBuilder().apply {
            parameters?.forEach {
                addParameter(it.type, it.name)
                addStatement("this.\$N = \$N", it.name, it.name)
            }
        }.build())

        Flavor.values().forEach {
            addEnumConstant(
                it.name,
                TypeSpec.anonymousClassBuilder(
                    "\$T.\$N, \$S, \$S",
                    it.dimension.javaClass,
                    it.dimension.name,
                    it.applicationIdSuffix,
                    it.versionNameSuffix
                ).build()
            )
        }
    }.build()

    JavaFile.builder(pkg, flavor).build().writeTo(File(path))
}