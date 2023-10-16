plugins {
    alias(libs.plugins.cashbook.android.library)
}

android {
    namespace = "com.github.mikephil.charting"
}

dependencies {
    implementation(libs.androidx.annotation)
}
