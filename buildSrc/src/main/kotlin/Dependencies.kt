@file:Suppress("unused", "MemberVisibilityCanBePrivate")

/**
 * 三方依赖配置
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/5/10
 */
object Dependencies {

    /** Maven 仓库地址 */
    object MavenRepository {

        /**
         * Jitpack 仓库
         * - [首页](https://www.jitpack.io/)
         */
        const val jitpack = "https://jitpack.io"

        /**
         * 阿里云仓库
         * - [指南](https://developer.aliyun.com/mvn/guide)
         */
        object AliYun {
            /** [central] 和 [jcenter] 的聚合 */
            const val public = "https://maven.aliyun.com/nexus/content/groups/public"
            const val central = "https://maven.aliyun.com/repository/central"
            const val jcenter = "https://maven.aliyun.com/repository/public"
            const val google = "https://maven.aliyun.com/repository/google"
            const val gradlePlugin = "https://maven.aliyun.com/repository/gradle-plugin"
            const val spring = "https://maven.aliyun.com/repository/spring"
            const val springPlugin = "https://maven.aliyun.com/repository/spring-plugin"
            const val grailsCore = "https://maven.aliyun.com/repository/grails-core"
            const val apacheSnapshots = "https://maven.aliyun.com/repository/apache-snapshots"
        }
    }

    /** 测试 */
    const val testJunit = "junit:junit:4.13"

    /**
     * Logger 日志打印
     * - [Github](https://github.com/orhanobut/logger)
     */
    const val logger = "com.orhanobut:logger:2.2.0"

    /**
     * Klaxon
     * - [Github](https://github.com/cbeust/klaxon)
     */
    const val klaxon = "com.beust:klaxon:5.4"

    /**
     * 美团多渠道
     * - [Github](https://github.com/Meituan-Dianping/walle)
     */
    const val walleChannel = "com.meituan.android.walle:library:1.1.7"

    /**
     * LiveEventBus
     * - [Github](https://github.com/JeremyLiao/LiveEventBus)
     */
    const val liveEventBus = "io.github.jeremyliao:live-event-bus:1.8.0"

    /**
     * TabLayout
     * - [Github](https://github.com/H07000223/FlycoTabLayout)
     */
    const val tabLayout = "com.flyco.tablayout:FlycoTabLayout_Lib:2.1.2@aar"

    /**
     * jsoup HTML 解析
     * - [Github](https://github.com/jhy/jsoup)
     */
    const val jsoup = "org.jsoup:jsoup:1.15.2"

    /**
     * 图表控件
     * - [Github](https://github.com/PhilJay/MPAndroidChart)
     */
    const val mpChart = "com.github.PhilJay:MPAndroidChart:v3.1.0"

    /**
     * Kotlin 相关
     * - [指南](https://kotlinlang.org/)
     */
    object Kotlin {

        /** Kotlin 版本 */
        const val version = "1.7.10"

        /** 标准库 */
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$version"

        /** jdk7 */
        const val jdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"

        /** jdk8 */
        const val jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"

        /** 反射 */
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    }

    /**
     * Kotlinx 相关
     */
    object Kotlinx {

        /**
         * kotlin 协程
         * - [指南](https://www.kotlincn.net/docs/reference/coroutines/coroutines-guide.html)
         * - [Github](https://github.com/Kotlin/kotlinx.coroutines)
         */
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"

        /**
         * kotlin Json 序列化
         * - [Github](https://github.com/Kotlin/kotlinx.serialization)
         */
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3"
    }

    /**
     * Androidx 相关
     */
    object Androidx {

        /**
         * legacy
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.legacy%3Alegacy-support-v4)
         */
        const val legacy = "androidx.legacy:legacy-support-v4:1.0.0"

        /**
         * appcompat
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.appcompat%3Aappcompat)
         */
        const val appcompat = "androidx.appcompat:appcompat:1.4.2"

        /**
         * recyclerview
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.recyclerview%3Arecyclerview)
         */
        const val recyclerview = "androidx.recyclerview:recyclerview:1.2.1"

        /**
         * viewpager2
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.viewpager2%3Aviewpager2)
         */
        const val viewpager2 = "androidx.viewpager2:viewpager2:1.0.0"

        /**
         * 约束性布局
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.constraintlayout%3Aconstraintlayout)
         */
        const val constraint = "androidx.constraintlayout:constraintlayout:2.1.4"

        /**
         * multidex 分包
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.multidex%3Amultidex)
         */
        const val multidex = "androidx.multidex:multidex:2.0.1"

        /**
         * 测试相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.test)
         */
        object Test {

            /** Test 版本号 */
            private const val version = "1.3.0"

            const val core = "androidx.test:core:$version"
            const val coreKtx = "androidx.test:core-ktx:$version"
            const val monitor = "androidx.test:monitor:$version"
            const val orchestrator = "androidx.test:orchestrator:$version"
            const val rules = "androidx.test:rules:$version"
            const val runner = "androidx.test:runner:$version"

            /**
             * Espresso
             * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.test.espresso)
             */
            object Espresso {

                /** espresso 版本号 */
                private const val version = "3.3.0"

                const val core = "androidx.test.espresso:espresso-core:$version"
                const val accessibility = "androidx.test.espresso:espresso-accessibility:$version"
                const val contrib = "androidx.test.espresso:espresso-contrib:$version"
                const val idlingResource = "androidx.test.espresso:espresso-idling-resource:$version"
                const val intents = "androidx.test.espresso:espresso-intents:$version"
                const val remote = "androidx.test.espresso:espresso-remote:$version"
                const val web = "androidx.test.espresso:espresso-web:$version"

                /**
                 * idling
                 * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.test.espresso.idling)
                 */
                object Idling {

                    /** idling 版本号 */
                    private const val version = "3.3.0"

                    const val concurrent = "androidx.test.espresso.idling:idling-concurrent:$version"
                    const val net = "androidx.test.espresso.idling:idling-net:$version"
                }
            }

            /**
             * Ext 相关
             * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.test.ext)
             */
            object Ext {

                /** Ext 版本号 */
                private const val version = "1.1.2"

                const val junit = "androidx.test.ext:junit:$version"
                const val junitKtx = "androidx.test.ext:junit-ktx:$version"
                const val truth = "androidx.test.ext:truth:$version"
            }

            const val ext_junit = "androidx.test.ext:junit:1.1.1"
            const val espresso_core = "androidx.test.espresso:espresso-core:3.2.0"
        }

        /**
         * Core 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.core)
         */
        object Core {

            /** core 版本 */
            private const val version = "1.8.0"

            /** 核心功能 */
            const val core = "androidx.core:core:$version"

            /** kotlin 拓展 */
            const val ktx = "androidx.core:core-ktx:$version"
        }

        /**
         * Compose 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.compose)
         */
        object Compose {

            /** Compose 版本号 */
            const val version = "1.0.0"

            /** 基础 */
            const val foundation = "androidx.compose.foundation:foundation:$version"

            /** UI 相关 */
            const val ui = "androidx.compose.ui:ui:$version"
            const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
            const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview:$version"
            const val uiTestJunit = "androidx.compose.ui:ui-test-junit4:$version"

            /** 运行时 */
            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata:$version"

            /** Material 控件 */
            const val material = "androidx.compose.material:material:$version"

            /** 布局 */
            const val layout = "androidx.compose.foundation:foundation-layout:$version"

            /** 动画 */
            const val animation = "androidx.compose.animation:animation:$version"
        }

        /**
         * Activity 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.activity)
         */
        object Activity {

            /** activity 版本 */
            private const val version = "1.5.0"

            /** Activity 相关 */
            const val activity = "androidx.activity:activity:$version"

            /** kotlin 拓展 */
            const val ktx = "androidx.activity:activity-ktx:$version"

            /** Compose 相关 */
            const val compose = "androidx.activity:activity-compose:1.3.1"
        }

        /**
         * Fragment 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.fragment)
         */
        object Fragment {

            /** fragment 版本 */
            private const val version = "1.5.0"

            /** Fragment 相关 */
            const val fragment = "androidx.fragment:fragment:$version"

            /** Kotlin 拓展 */
            const val ktx = "androidx.fragment:fragment-ktx:$version"

            /** 测试 */
            const val testing = "androidx.fragment:fragment-testing:$version"
        }

        /**
         * 生命周期相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.lifecycle)
         */
        object Lifecycle {

            /** lifecycle 版本 */
            private const val version = "2.5.0"

            const val reactivestreams = "androidx.lifecycle:lifecycle-reactivestreams:$version"
            const val reactivestreamsKtx = "androidx.lifecycle:lifecycle-reactivestreams-ktx:$version"
            const val process = "androidx.lifecycle:lifecycle-process:$version"
            const val common = "androidx.lifecycle:lifecycle-common:$version"
            const val commonJava8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val liveData = "androidx.lifecycle:lifecycle-livedata:$version"
            const val liveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val liveDataCore = "androidx.lifecycle:lifecycle-livedata-core:$version"
            const val liveDataCoreKtx = "androidx.lifecycle:lifecycle-livedata-core-ktx:$version"
            const val runtime = "androidx.lifecycle:lifecycle-runtime:$version"
            const val runtimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val runtimeTesting = "androidx.lifecycle:lifecycle-runtime-testing:$version"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
            const val service = "androidx.lifecycle:lifecycle-service:$version"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel:$version"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val viewModelSavedState = "androidx.lifecycle:lifecycle-viewmodel-savedstate:$version"

            const val extensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
            const val viewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha02"
        }

        /**
         * Room 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.room)
         */
        object Room {

            /** room 版本 */
            private const val version = "2.4.2"

            const val runtime = "androidx.room:room-runtime:$version"
            const val compiler = "androidx.room:room-compiler:$version"
            const val compilerProcessing = "androidx.room:room-compiler-processing:$version"
            const val ktx = "androidx.room:room-ktx:$version"
            const val rxjava2 = "androidx.room:room-rxjava2:$version"
            const val rxjava3 = "androidx.room:room-rxjava3:$version"
            const val common = "androidx.room:room-common:$version"
            const val guava = "androidx.room:room-guava:$version"
            const val migration = "androidx.room:room-migration:$version"
            const val testing = "androidx.room:room-testing:$version"

            const val coroutines = "androidx.room:room-coroutines:2.1.0-alpha04"
        }

        /**
         * Paging 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.paging)
         */
        object Paging {

            /** paging 版本 */
            private const val version = "3.1.1"

            const val runtime = "androidx.paging:paging-runtime:$version"
            const val runtimeKtx = "androidx.paging:paging-runtime-ktx:$version"
            const val common = "androidx.paging:paging-common:$version"
            const val commonKtx = "androidx.paging:paging-common-ktx:$version"
            const val rxjava2 = "androidx.paging:paging-rxjava2:$version"
            const val rxjava2Ktx = "androidx.paging:paging-rxjava2-ktx:$version"
            const val guava = "androidx.paging:paging-guava:$version"
            const val rxjava3 = "androidx.paging:paging-rxjava3:$version"

            const val compose = "androidx.paging:paging-compose:1.0.0-alpha08"
        }

        /**
         * Navigation 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.navigation)
         */
        object Navigation {

            /** navigation 版本 */
            private const val version = "2.3.5"

            const val fragment = "androidx.navigation:navigation-fragment:$version"
            const val fragmentKtx = "androidx.navigation:navigation-fragment-ktx:$version"
            const val safeArgsGenerator = "androidx.navigation:navigation-safe-args-generator:$version"
            const val safeArgsGradlePlugin = "androidx.navigation:navigation-safe-args-gradle-plugin:$version"
            const val ui = "androidx.navigation:navigation-ui:$version"
            const val uiKtx = "androidx.navigation:navigation-ui-ktx:$version"
            const val runtime = "androidx.navigation:navigation-runtime:$version"
            const val runtimeKtx = "androidx.navigation:navigation-runtime-ktx:$version"
            const val common = "androidx.navigation:navigation-common:$version"
            const val commonKtx = "androidx.navigation:navigation-common-ktx:$version"
            const val testing = "androidx.navigation:navigation-testing:$version"
            const val dynamicFeaturesFragment = "androidx.navigation:navigation-dynamic-features-fragment:$version"
            const val dynamicFeaturesRuntime = "androidx.navigation:navigation-dynamic-features-runtime:$version"

            const val compose = "androidx.navigation:navigation-compose:1.0.0-alpha08"
        }

        /**
         * WorkManager 相关
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=androidx.work)
         */
        object Work {

            /** 版本号 */
            const val version = "2.7.1"

            const val runtime = "androidx.work:work-runtime:$version"
            const val runtimeKtx = "androidx.work:work-runtime-ktx:$version"
            const val rxjava2 = "androidx.work:work-rxjava2:$version"
            const val rxjava3 = "androidx.work:work-rxjava3:$version"
            const val testing = "androidx.work:work-testing:$version"
        }
    }

    /**
     * Google 相关
     */
    object Google {

        /**
         * androidx material
         * - [版本查询](https://www.wanandroid.com/maven_pom/index?k=com.google.android.material)
         */
        const val material = "com.google.android.material:material:1.6.1"

        /**
         * Zxing
         * - [Github](https://github.com/zxing/zxing)
         */
        const val zxing = "com.google.zxing:core:3.4.1"

        /**
         * Code 相关
         */
        object Code {

            /** Findbugs Jsr */
            const val findbugs_jsr = "com.google.code.findbugs:jsr305:3.0.2"

            /**
             * Gson
             * - [Github](https://github.com/google/gson)
             */
            const val gson = "com.google.code.gson:gson:2.9.0"
        }

        /** Exoplayer */
        object Exoplayer {

            /** 版本号 */
            private const val version = "2.8.4"

            /** 全功能 */
            const val exoplayer = "com.google.android.exoplayer:exoplayer:$version"

            /** 核心功能（必须） */
            const val core = "com.google.android.exoplayer:exoplayer-core:$version"

            /** DASH */
            const val dash = "com.google.android.exoplayer:exoplayer-dash:$version"

            /** HLS */
            const val hls = "com.google.android.exoplayer:exoplayer-hls:$version"

            /** SmoothStreaming */
            const val smoothstreaming = "com.google.android.exoplayer:exoplayer-smoothstreaming:$version"

            /** 媒体转换 */
            const val transformer = "com.google.android.exoplayer:exoplayer-transformer:$version"

            /** UI 组件 */
            const val ui = "com.google.android.exoplayer:exoplayer-ui:$version"
        }
    }

    /**
     * Moshi
     * - [Github](https://github.com/square/moshi)
     */
    object Moshi {

        /** moshi 版本 */
        private const val version = "1.11.0"

        const val moshi = "com.squareup.moshi:moshi:$version"
        const val moshiKtx = "com.squareup.moshi:moshi-kotlin:$version"
    }

    /**
     * Koin
     * - [Github](https://github.com/InsertKoinIO/koin)
     */
    object Koin2 {

        /** koin 版本 */
        private const val version = "2.2.2"

        const val scope = "org.koin:koin-androidx-scope:$version"
        const val viewModel = "org.koin:koin-androidx-viewmodel:$version"
        const val fragment = "org.koin:koin-androidx-fragment:$version"
        const val workManager = "org.koin:koin-androidx-workmanager:$version"
        const val compose = "org.koin:koin-androidx-compose:$version"
        const val ext = "org.koin:koin-androidx-ext:$version"
    }


    /**
     * Koin3
     * - [Github](https://github.com/InsertKoinIO/koin)
     * - [指南](https://insert-koin.io/)
     */
    object Koin3 {

        /** koin 版本 */
        private const val version = "3.2.0"

        const val android = "io.insert-koin:koin-android:$version"

        //        const val androidExt = "io.insert-koin:koin-android-ext:$version"
        const val androidExt = "io.insert-koin:koin-android-ext:3.0.2"
        const val workManager = "io.insert-koin:koin-androidx-workmanager:$version"
        const val compose = "io.insert-koin:koin-androidx-compose:$version"
    }

    /**
     * Glide
     * - [Github](https://github.com/bumptech/glide)
     */
    object Glide {

        /** glide 版本 */
        private const val version = "4.12.0"

        const val glide = "com.github.bumptech.glide:glide:$version"
        const val compiler = "com.github.bumptech.glide:compiler:$version"
    }

    /**
     * Suqareup 相关
     */
    object Squareup {

        /**
         * okio
         * - [Github](https://github.com/square/okio)
         */
        const val okio = "com.squareup.okio:okio:2.10.0"

        /**
         * okhttp
         * - [Github](https://github.com/square/okhttp)
         * - [指南](https://square.github.io/okhttp)
         */
        object OkHttp {

            /** okhttp 版本 */
            private const val version = "4.10.0"

            const val okhttp = "com.squareup.okhttp3:okhttp:$version"
            const val urlConnection = "com.squareup.okhttp3:okhttp-urlconnection:$version"
            const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
        }

        /**
         * Retrofit
         * - [Github](https://github.com/square/retrofit)
         * - [指南](https://square.github.io/retrofit/)
         */
        object Retrofit {

            /** retrofit 版本 */
            private const val version = "2.9.0"

            const val retrofit = "com.squareup.retrofit2:retrofit:$version"
            const val converterGson = "com.squareup.retrofit2:converter-gson:$version"
            const val converterMoshi = "com.squareup.retrofit2:converter-moshi:$version"
            const val converterKt =
                "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0"
        }
    }

    /**
     * SmartRefreshLayout
     * - [Github](https://github.com/scwang90/SmartRefreshLayout)
     */
    object SmartRefresh {

        /** smart refresh 版本 */
        private const val version = "2.0.3"

        const val smartRefresh = "com.scwang.smart:refresh-layout-kernel:$version"
        const val headerClassics = "com.scwang.smart:refresh-header-classics:$version"
        const val footerClassics = "com.scwang.smart:refresh-footer-classics:$version"
    }

    /**
     * 状态栏工具
     * - [Github](https://github.com/gyf-dev/ImmersionBar)
     */
    object ImmersionBar {

        /** immersion bar 版本 */
        private const val version = "3.0.0"

        const val immersionBar = "com.gyf.immersionbar:immersionbar:$version"
        const val ktx = "com.gyf.immersionbar:immersionbar-ktx:$version"

        object MarvenCenter {
            /** immersion bar 版本 */
            private const val version = "3.2.2"

            const val immersionBar = "com.geyifeng.immersionbar:immersionbar:$version"
            const val ktx = "com.geyifeng.immersionbar:immersionbar-ktx:$version"

        }
    }

    /**
     * Coil 图片加载
     * - [Github](https://github.com/coil-kt/coil)
     * - [指南](https://coil-kt.github.io/coil)
     */
    object Coil {

        /** coil 版本 */
        private const val version = "2.1.0"

        const val coil = "io.coil-kt:coil:$version"
        const val base = "io.coil-kt:coil-base:$version"
        const val gif = "io.coil-kt:coil-gif:$version"
        const val svg = "io.coil-kt:coil-svg:$version"
        const val video = "io.coil-kt:coil-video:$version"
    }

    /**
     * 换肤支持
     * - [Github](https://github.com/ximsfei/Android-skin-support)
     */
    object SkinSupport {

        /** 换肤支持版本 */
        private const val version = "4.0.5"

        const val skinSupport = "skin.support:skin-support:$version"
        const val appcompat = "skin.support:skin-support-appcompat:$version"
        const val material = "skin.support:skin-support-design:$version"
        const val cardView = "skin.support:skin-support-cardview:$version"
        const val constraint = "skin.support:skin-support-constraint-layout:$version"
    }

    /**
     * 阿里
     */
    object Alibaba {

        /**
         * ARouter 路由
         * - [Github](https://github.com/alibaba/ARouter)
         */
        object ARouter {

            /** ARouter 版本 */
            private const val version = "1.5.2"

            const val api = "com.alibaba:arouter-api:$version"
            const val compiler = "com.alibaba:arouter-compiler:$version"
        }
    }

    /**
     * 腾讯
     */
    object Tencent {

        /**
         * MMKV
         * - [Github](https://github.com/tencent/mmkv)
         */
        const val mmkv = "com.tencent:mmkv:1.2.13"
    }

    /** 滴滴 */
    object Didi {

        /**
         * Doraemonkit
         * - [Github](https://github.com/Android-MI/DoraemonKit)
         */
        object DoraemonKit {
            /** 版本号 */
            private const val version = "1.1.2"

            const val debug = "com.didichuxing.doraemonkit:doraemonkit:$version"
            const val release = "com.didichuxing.doraemonkit:doraemonkit-no-op:$version"
        }
    }

    /**
     * Markdown 解析
     * - [Github](https://github.com/noties/Markwon)
     * - [指南](https://noties.io/Markwon/)
     */
    object Markwon {

        private const val version = "4.6.2"

        const val markwon = "io.noties.markwon:core:$version"
    }

    /**
     * 日历控件
     * - [Github](https://github.com/huanghaibin-dev/CalendarView)
     */
    const val calendar_view = "com.haibin:calendarview:3.7.1"
}