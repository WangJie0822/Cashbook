package cn.wj.android.cashbook.feature.tags.model

import cn.wj.android.cashbook.core.model.model.TagModel

sealed class TagDialogState {

    class Edit(val tag: TagModel?) : TagDialogState()

    class Delete(val tag: TagModel) : TagDialogState()
}