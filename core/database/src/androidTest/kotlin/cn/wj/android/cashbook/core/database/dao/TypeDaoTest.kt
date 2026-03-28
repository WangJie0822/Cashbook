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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TypeDao 数据库操作测试
 */
@RunWith(AndroidJUnit4::class)
class TypeDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var typeDao: TypeDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        typeDao = database.typeDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun when_insertType_then_queryByIdReturnsIt() = runTest {
        // 插入一条类型记录
        val type = TypeTable(
            id = null,
            parentId = -1L,
            name = "餐饮",
            iconName = "icon_food",
            typeLevel = 0,
            typeCategory = 0,
            protected = SWITCH_INT_OFF,
            sort = 1,
        )
        val insertedId = typeDao.insertType(type)

        // 通过 ID 查询，验证数据正确
        val result = typeDao.queryById(insertedId)
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(insertedId)
        assertThat(result.name).isEqualTo("餐饮")
        assertThat(result.iconName).isEqualTo("icon_food")
        assertThat(result.parentId).isEqualTo(-1L)
        assertThat(result.typeLevel).isEqualTo(0)
        assertThat(result.typeCategory).isEqualTo(0)
        assertThat(result.protected).isEqualTo(SWITCH_INT_OFF)
        assertThat(result.sort).isEqualTo(1)
    }

    @Test
    fun when_insertOrReplace_then_updatesExisting() = runTest {
        // 先插入一条类型记录
        val type = TypeTable(
            id = null,
            parentId = -1L,
            name = "原始类型",
            iconName = "icon_old",
            typeLevel = 0,
            typeCategory = 0,
            protected = SWITCH_INT_OFF,
            sort = 0,
        )
        val insertedId = typeDao.insertType(type)

        // 使用相同 ID 调用 insertOrReplace 更新数据
        val updatedType = TypeTable(
            id = insertedId,
            parentId = -1L,
            name = "更新后类型",
            iconName = "icon_new",
            typeLevel = 0,
            typeCategory = 1,
            protected = SWITCH_INT_ON,
            sort = 5,
        )
        typeDao.insertOrReplace(updatedType)

        // 验证记录已更新且总数不变
        val result = typeDao.queryById(insertedId)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("更新后类型")
        assertThat(result.iconName).isEqualTo("icon_new")
        assertThat(result.typeCategory).isEqualTo(1)
        assertThat(result.protected).isEqualTo(SWITCH_INT_ON)
        assertThat(result.sort).isEqualTo(5)
    }

    @Test
    fun when_insertTypes_then_queryAllEmitsAll() = runTest {
        // 批量插入多条类型
        val type1 = TypeTable(
            id = null,
            parentId = -1L,
            name = "餐饮",
            iconName = "icon_food",
            typeLevel = 0,
            typeCategory = 0,
            protected = SWITCH_INT_OFF,
            sort = 0,
        )
        val type2 = TypeTable(
            id = null,
            parentId = -1L,
            name = "交通",
            iconName = "icon_transport",
            typeLevel = 0,
            typeCategory = 0,
            protected = SWITCH_INT_OFF,
            sort = 1,
        )
        val type3 = TypeTable(
            id = null,
            parentId = -1L,
            name = "工资",
            iconName = "icon_salary",
            typeLevel = 0,
            typeCategory = 1,
            protected = SWITCH_INT_OFF,
            sort = 0,
        )
        typeDao.insertTypes(type1, type2, type3)

        // 通过 Flow 获取所有类型
        val allTypes = typeDao.queryAll().first()
        assertThat(allTypes).hasSize(3)
        val names = allTypes.map { it.name }
        assertThat(names).containsExactly("餐饮", "交通", "工资")
    }

    @Test
    fun when_queryByTypeCategory_then_returnsMatchingTypes() = runTest {
        // 插入不同分类的类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "餐饮",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "购物",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 1,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "工资",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 1,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "转账",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 2,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 查询支出类型（typeCategory=0）
        val expenditureTypes = typeDao.queryByTypeCategory(0).first()
        assertThat(expenditureTypes).hasSize(2)
        assertThat(expenditureTypes.map { it.name }).containsExactly("餐饮", "购物")

        // 查询收入类型（typeCategory=1）
        val incomeTypes = typeDao.queryByTypeCategory(1).first()
        assertThat(incomeTypes).hasSize(1)
        assertThat(incomeTypes[0].name).isEqualTo("工资")

        // 查询转账类型（typeCategory=2）
        val transferTypes = typeDao.queryByTypeCategory(2).first()
        assertThat(transferTypes).hasSize(1)
        assertThat(transferTypes[0].name).isEqualTo("转账")
    }

    @Test
    fun when_queryByLevelAndTypeCategory_then_returnsMatchingTypes() = runTest {
        // 插入不同级别和分类的类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级支出",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = 1L,
                name = "二级支出",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级收入",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 1,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 查询一级支出类型
        val result = typeDao.queryByLevelAndTypeCategory(typeLevel = 0, typeCategory = 0)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("一级支出")

        // 查询二级支出类型
        val secondLevel = typeDao.queryByLevelAndTypeCategory(typeLevel = 1, typeCategory = 0)
        assertThat(secondLevel).hasSize(1)
        assertThat(secondLevel[0].name).isEqualTo("二级支出")
    }

    @Test
    fun when_queryByLevel_then_returnsMatchingTypes() = runTest {
        // 插入一级和二级类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级A",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级B",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 1,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = 1L,
                name = "二级A",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 查询一级类型
        val firstLevel = typeDao.queryByLevel(0)
        assertThat(firstLevel).hasSize(2)
        assertThat(firstLevel.map { it.name }).containsExactly("一级A", "一级B")

        // 查询二级类型
        val secondLevel = typeDao.queryByLevel(1)
        assertThat(secondLevel).hasSize(1)
        assertThat(secondLevel[0].name).isEqualTo("二级A")
    }

    @Test
    fun when_queryByParentId_then_returnsChildTypes() = runTest {
        // 插入父类型
        val parentId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "父类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入子类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型A",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型B",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 1,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "其他类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 查询指定父类型下的子类型
        val children = typeDao.queryByParentId(parentId)
        assertThat(children).hasSize(2)
        assertThat(children.map { it.name }).containsExactly("子类型A", "子类型B")
    }

    @Test
    fun when_queryByName_then_returnsMatchingType() = runTest {
        // 插入类型
        typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "唯一类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 按名称查询
        val result = typeDao.queryByName("唯一类型")
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("唯一类型")

        // 查询不存在的名称
        val notFound = typeDao.queryByName("不存在")
        assertThat(notFound).isNull()
    }

    @Test
    fun when_updateTypeLevel_then_updatesFields() = runTest {
        // 插入一条二级类型
        val typeId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = 10L,
                name = "原始类型",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 更新类型级别和父类型
        typeDao.updateTypeLevel(id = typeId, parentId = 20L, typeLevel = 0)

        // 验证更新后的数据
        val result = typeDao.queryById(typeId)
        assertThat(result).isNotNull()
        assertThat(result!!.parentId).isEqualTo(20L)
        assertThat(result.typeLevel).isEqualTo(0)
        // 其他字段不受影响
        assertThat(result.name).isEqualTo("原始类型")
    }

    @Test
    fun when_deleteById_then_typeIsRemoved() = runTest {
        // 插入两条类型
        val id1 = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "类型一",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )
        typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "类型二",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 1,
            ),
        )

        // 删除第一条
        typeDao.deleteById(id1)

        // 验证仅剩第二条
        assertThat(typeDao.queryById(id1)).isNull()
        val allTypes = typeDao.queryAll().first()
        assertThat(allTypes).hasSize(1)
        assertThat(allTypes[0].name).isEqualTo("类型二")
    }

    @Test
    fun when_countByName_then_returnsCorrectCount() = runTest {
        // 插入同名和不同名的类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "重复类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "重复类型",
                iconName = "icon2",
                typeLevel = 0,
                typeCategory = 1,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "唯一类型",
                iconName = "icon3",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        assertThat(typeDao.countByName("重复类型")).isEqualTo(2)
        assertThat(typeDao.countByName("唯一类型")).isEqualTo(1)
        assertThat(typeDao.countByName("不存在")).isEqualTo(0)
    }

    @Test
    fun when_countByLevel_then_returnsCorrectCount() = runTest {
        // 插入不同级别的类型
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级A",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = -1L,
                name = "一级B",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 1,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = 1L,
                name = "二级A",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        assertThat(typeDao.countByLevel(0)).isEqualTo(2)
        assertThat(typeDao.countByLevel(1)).isEqualTo(1)
        assertThat(typeDao.countByLevel(2)).isEqualTo(0)
    }

    @Test
    fun when_countByParentId_then_returnsCorrectCount() = runTest {
        // 插入父类型和子类型
        val parentId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "父类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )
        typeDao.insertTypes(
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型A",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型B",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 1,
            ),
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型C",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 2,
            ),
        )

        assertThat(typeDao.countByParentId(parentId)).isEqualTo(3)
        assertThat(typeDao.countByParentId(999L)).isEqualTo(0)
    }

    @Test
    fun when_promoteChildTypes_then_childrenBecomeFirstLevel() = runTest {
        // 插入父类型
        val parentId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "父类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入子类型
        val childId1 = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型A",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )
        val childId2 = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = parentId,
                name = "子类型B",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 1,
            ),
        )

        // 插入不属于该父类型的子类型（不应被影响）
        val otherChildId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = 999L,
                name = "其他子类型",
                iconName = "icon",
                typeLevel = 1,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 提升子类型为一级类型
        typeDao.promoteChildTypes(parentId)

        // 验证子类型已变为一级类型
        val child1 = typeDao.queryById(childId1)
        assertThat(child1).isNotNull()
        assertThat(child1!!.parentId).isEqualTo(-1L)
        assertThat(child1.typeLevel).isEqualTo(0)

        val child2 = typeDao.queryById(childId2)
        assertThat(child2).isNotNull()
        assertThat(child2!!.parentId).isEqualTo(-1L)
        assertThat(child2.typeLevel).isEqualTo(0)

        // 验证不属于该父类型的子类型未被影响
        val otherChild = typeDao.queryById(otherChildId)
        assertThat(otherChild).isNotNull()
        assertThat(otherChild!!.parentId).isEqualTo(999L)
        assertThat(otherChild.typeLevel).isEqualTo(1)
    }

    @Test
    fun when_updateRecordTypeId_then_recordsAreUpdated() = runTest {
        // 插入旧类型和新类型
        val oldTypeId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "旧类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )
        val newTypeId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "新类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入使用旧类型的记录
        val recordId1 = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = oldTypeId,
                assetId = -1L,
                intoAssetId = -1L,
                booksId = 1L,
                amount = 10000L,
                finalAmount = 10000L,
                concessions = 0L,
                charge = 0L,
                remark = "记录1",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )
        val recordId2 = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = oldTypeId,
                assetId = -1L,
                intoAssetId = -1L,
                booksId = 1L,
                amount = 20000L,
                finalAmount = 20000L,
                concessions = 0L,
                charge = 0L,
                remark = "记录2",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 更新记录的类型 ID
        typeDao.updateRecordTypeId(oldTypeId, newTypeId)

        // 验证记录的类型 ID 已更新
        val record1 = transactionDao.queryRecordById(recordId1)
        assertThat(record1).isNotNull()
        assertThat(record1!!.typeId).isEqualTo(newTypeId)

        val record2 = transactionDao.queryRecordById(recordId2)
        assertThat(record2).isNotNull()
        assertThat(record2!!.typeId).isEqualTo(newTypeId)
    }

    @Test
    fun when_countRecordsByTypeId_then_returnsCorrectCount() = runTest {
        // 插入类型
        val typeId1 = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "类型一",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )
        val typeId2 = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "类型二",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入引用不同类型的记录
        transactionDao.insertRecord(
            RecordTable(
                id = null, typeId = typeId1, assetId = -1L, intoAssetId = -1L,
                booksId = 1L, amount = 1000L, finalAmount = 1000L, concessions = 0L,
                charge = 0L, remark = "", reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )
        transactionDao.insertRecord(
            RecordTable(
                id = null, typeId = typeId1, assetId = -1L, intoAssetId = -1L,
                booksId = 1L, amount = 2000L, finalAmount = 2000L, concessions = 0L,
                charge = 0L, remark = "", reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )
        transactionDao.insertRecord(
            RecordTable(
                id = null, typeId = typeId2, assetId = -1L, intoAssetId = -1L,
                booksId = 1L, amount = 3000L, finalAmount = 3000L, concessions = 0L,
                charge = 0L, remark = "", reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 验证各类型的记录计数
        assertThat(typeDao.countRecordsByTypeId(typeId1)).isEqualTo(2)
        assertThat(typeDao.countRecordsByTypeId(typeId2)).isEqualTo(1)
        assertThat(typeDao.countRecordsByTypeId(999L)).isEqualTo(0)
    }
}
