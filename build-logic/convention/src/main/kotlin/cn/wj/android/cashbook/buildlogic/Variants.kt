/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("EnumValuesSoftDeprecate")

package cn.wj.android.cashbook.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 配置多渠道
 *
 * - Application 使用，配置多渠道并生成渠道枚举类
 */
fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: CashbookFlavor) -> Unit = {},
) {
    commonExtension.apply {
        // 配置维度
        flavorDimensions += CashbookFlavorDimension.values().map { it.name }

        // 多渠道配置
        productFlavors.apply {
            CashbookFlavor.values().forEach {
                create(it.name) {
                    dimension = it.dimension.name
                    flavorConfigurationBlock(this, it)
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        it.applicationIdSuffix?.let { suffix ->
                            applicationIdSuffix = suffix
                        }

                        it.versionNameSuffix?.let { suffix ->
                            versionNameSuffix = suffix
                        }
                    }
                }
            }
        }
    }
}

/**
 * 为 Library 模块生成多渠道枚举类 [CashbookFlavor] 到 `<namespace>.buildlogic` 包下。
 *
 * AGP 9 移除 `libraryVariants`/`generateBuildConfigProvider`，改用 `androidComponents.onVariants` +
 * 自定义 [GenerateFlavorTask] + `variant.sources.java.addGeneratedSourceDirectory` 注入生成源码目录。
 */
fun Project.configureGenerateFlavors() {
    val project = this
    configure<LibraryAndroidComponentsExtension> {
        onVariants { variant ->
            val pkgProvider = variant.namespace.map { "$it.buildlogic" }
            val capName = variant.name.replaceFirstChar { it.uppercase() }
            val genTask = project.tasks.register<GenerateFlavorTask>("generate${capName}FlavorEnum") {
                packageName.set(pkgProvider)
            }
            variant.sources.java?.addGeneratedSourceDirectory(genTask, GenerateFlavorTask::outputDir)
        }
    }
}

/** 生成渠道枚举类的 Task：写 [CashbookFlavor] 到 [outputDir]，包名 [packageName]。 */
@DisableCachingByDefault(because = "生成小型枚举源文件，缓存收益不值；validatePlugins enableStricterValidation 要求显式缓存注解")
abstract class GenerateFlavorTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        generateFlavor(packageName.get(), dir.path)
    }
}

/** 将多渠道枚举类生成到指定路径 [path] [pkg] 包下 */
fun generateFlavor(pkg: String, path: String) {
    val flavor = TypeSpec.enumBuilder(CashbookFlavor::class.java.simpleName).apply {
        addModifiers(Modifier.PUBLIC)
        CashbookFlavor.values().forEach {
            addEnumConstant(it.name)
        }
    }.build()
    JavaFile.builder(pkg, flavor).build().writeTo(File(path))
}
