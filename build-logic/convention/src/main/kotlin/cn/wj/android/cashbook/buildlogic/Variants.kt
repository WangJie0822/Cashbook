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

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 配置多渠道
 *
 * - Application 使用，配置多渠道并生成渠道枚举类
 */
fun configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: CashbookFlavor) -> Unit = {},
) {
    commonExtension.apply {
        // 配置维度
        flavorDimensions += CashbookFlavorDimension.values().map { it.name }

        // 多渠道配置
        productFlavors {
            CashbookFlavor.values().forEach {
                create(it.name) {
                    dimension = it.dimension.name
                    flavorConfigurationBlock(this, it)
                    if (this@apply is BaseAppModuleExtension && this is ApplicationProductFlavor) {
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

inline fun <reified T : CommonExtension<*, *, *, *, *, *>> Project.configureGenerateFlavors() =
    configure<T> {
        println("> Task :${project.name}:configureFlavors generateFlavorFile")
        when (this) {
            is BaseAppModuleExtension -> applicationVariants
            is LibraryExtension -> libraryVariants
            else -> throw RuntimeException("Unsupported project extension $this ${T::class}")
        }.all {
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
