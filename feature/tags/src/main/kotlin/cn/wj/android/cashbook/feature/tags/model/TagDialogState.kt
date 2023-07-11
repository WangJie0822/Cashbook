package cn.wj.android.cashbook.feature.tags.model

import cn.wj.android.cashbook.core.model.entity.TagEntity

sealed class TagDialogState {

    class Edit(val tag: TagEntity?) : TagDialogState()

    class Delete(val tag: TagEntity) : TagDialogState()
}