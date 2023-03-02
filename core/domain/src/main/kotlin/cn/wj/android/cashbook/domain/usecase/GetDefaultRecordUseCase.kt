package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.getIdByString
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 获取默认记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetDefaultRecordUseCase @Inject constructor(
    private val typeRepository: TypeRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {

    operator fun invoke(): Flow<RecordEntity> {
        // appData 获取默认参数
        return appPreferencesDataSource.appData.map { appDataModel ->
            // 获取默认类型，如果没有，去支出类型第一个
            val recordTypeById = typeRepository.getRecordTypeById(appDataModel.defaultTypeId)
                ?: typeRepository.getFirstRecordTypeListByCategory(RecordTypeCategoryEnum.EXPENDITURE)
                    .first()
            RecordEntity(
                id = -1L,
                booksId = appDataModel.currentBookId,
                typeCategory = recordTypeById.typeCategory,
                type = recordTypeById.asEntity(),
                assetId = appDataModel.lastAssetId,
                relatedAssetId = -1L,
                amount = "0",
                charges = "",
                concessions = "",
                remark = "",
                reimbursable = false,
                modifyTime = System.currentTimeMillis().dateFormat(DATE_FORMAT_NO_SECONDS),
            )
        }
    }

    private fun RecordTypeModel.asEntity(): RecordTypeEntity {
        return RecordTypeEntity(
            id = this.id,
            parentId = this.parentId,
            name = this.name,
            iconResId = getIdByString(this.iconName, "drawable"),
            sort = this.sort,
            child = listOf(),
            selected = false,
            shapeType = -1,
        )
    }
}