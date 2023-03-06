package cn.wj.android.cashbook.feature.tags.preview

import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.tags.screen.DeleteTagDialog
import cn.wj.android.cashbook.feature.tags.screen.EditTagDialog

@DevicePreviews
@Composable
fun EditTagDialogPreview() {
    CashbookTheme {
        EditTagDialog(tagEntity = null, onConfirm = {}, onDismiss = {})
    }
}

@DevicePreviews
@Composable
fun DeleteTagDialogPreview() {
    CashbookTheme {
        DeleteTagDialog(tagEntity = TagEntity(-1L, "测试", false), onConfirm = {}, onDismiss = {})
    }
}