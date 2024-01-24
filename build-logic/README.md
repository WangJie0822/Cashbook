# 约定插件

`build-logic`文件夹定义了特定于项目的约定插件，用于为公共模块配置保持单一的来源。

这种方法很大程度上是基于
[https://developer.squareup.com/blog/herding-elephants/](https://developer.squareup.com/blog/herding-elephants/)
和
[https://github.com/jjohannes/idiomatic-gradle](https://github.com/jjohannes/idiomatic-gradle).

通过在`build-logic`中设置约定插件，我们可以避免重复的构建脚本设置，混乱的`subproject`配置，而没有`buildSrc`目录的陷阱。

`build-logic`是一个包含的构建，在根目录下配置
[`settings.gradle.kts`](../settings.gradle.kts).

在`build-logic`内部是一个`convention`模块，它定义了一组插件，所有普通模块都可以使用它们来配置自己。

`build-logic`还包括一组用于在插件之间共享逻辑的`Kotlin`文件，这对于配置使用共享代码的Android组件(库vs应用程序)最有用。

这些插件是**可添加**和**可组合**的，并且只尝试完成单一的职责。然后模块可以挑选和选择它们需要的配置。

如果没有共享代码的模块有一次性逻辑，最好直接在模块的`build.gradle `中定义，而不是创建一个具有特定模块设置的约定插件。

约定插件的当前列表:

- [`cashbook.android.application`](convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt),
  [`cashbook.android.library`](convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt),
  [`cashbook.android.test`](convention/src/main/kotlin/AndroidTestConventionPlugin.kt):
  配置常用的Android和Kotlin选项
- [`cashbook.android.application.compose`](convention/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt),
  [`cashbook.android.library.compose`](convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt):
  配置Jetpack Compose选项
