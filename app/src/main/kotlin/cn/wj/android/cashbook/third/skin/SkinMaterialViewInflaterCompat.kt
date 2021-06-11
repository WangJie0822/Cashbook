//@file:Suppress("unused")
//
//package cn.wj.android.cashbook.third.skin
//
//import android.content.Context
//import android.util.AttributeSet
//import android.view.View
//import com.google.android.material.imageview.ShapeableImageView
//import skin.support.app.SkinLayoutInflater
//import skin.support.widget.SkinCompatBackgroundHelper
//import skin.support.widget.SkinCompatImageHelper
//import skin.support.widget.SkinCompatSupportable
//
///**
// * Material 库换肤兼容
// * * 用于兼容 [ShapeableImageView] 换肤
// *
// * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/4/19
// */
//class SkinMaterialViewInflaterCompat : SkinLayoutInflater {
//    override fun createView(context: Context, name: String?, attrs: AttributeSet): View? {
//        return if (name == "com.google.android.material.imageview.ShapeableImageView") {
//            SkinCompatShapeableImageView(context, attrs)
//        } else {
//            null
//        }
//    }
//}
//
//class SkinCompatShapeableImageView(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyle: Int = 0
//) : ShapeableImageView(context, attrs, defStyle), SkinCompatSupportable {
//
//    private val backgroundTintHelper = SkinCompatBackgroundHelper(this)
//    private val imageHelper = SkinCompatImageHelper(this)
//
//    init {
//        backgroundTintHelper.loadFromAttributes(attrs, defStyle)
//        imageHelper.loadFromAttributes(attrs, defStyle)
//    }
//
//    override fun setBackgroundResource(resId: Int) {
//        super.setBackgroundResource(resId)
//        backgroundTintHelper.onSetBackgroundResource(resId)
//    }
//
//    override fun setImageResource(resId: Int) {
//        super.setImageResource(resId)
//        imageHelper.setImageResource(resId)
//    }
//
//    override fun applySkin() {
//        backgroundTintHelper.applySkin()
//        imageHelper.applySkin()
//    }
//}