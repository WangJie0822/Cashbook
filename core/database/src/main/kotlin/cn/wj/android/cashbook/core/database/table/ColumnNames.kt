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

package cn.wj.android.cashbook.core.database.table

/* 资产 */
const val TABLE_ASSET = "db_asset"
const val TABLE_ASSET_ID = "id"
const val TABLE_ASSET_BOOKS_ID = "books_id"
const val TABLE_ASSET_NAME = "name"
const val TABLE_ASSET_BALANCE = "balance"
const val TABLE_ASSET_TOTAL_AMOUNT = "total_amount"
const val TABLE_ASSET_BILLING_DATE = "billing_date"
const val TABLE_ASSET_REPAYMENT_DATE = "repayment_date"
const val TABLE_ASSET_TYPE = "type"
const val TABLE_ASSET_CLASSIFICATION = "classification"
const val TABLE_ASSET_INVISIBLE = "invisible"
const val TABLE_ASSET_OPEN_BANK = "open_bank"
const val TABLE_ASSET_CARD_NO = "card_no"
const val TABLE_ASSET_REMARK = "remark"
const val TABLE_ASSET_SORT = "sort"
const val TABLE_ASSET_MODIFY_TIME = "modify_time"

/* 账本 */
const val TABLE_BOOKS = "db_books"
const val TABLE_BOOKS_ID = "id"
const val TABLE_BOOKS_NAME = "name"
const val TABLE_BOOKS_BG_URI = "bg_uri"
const val TABLE_BOOKS_DESCRIPTION = "description"
const val TABLE_BOOKS_MODIFY_TIME = "modify_time"

/* 记录 */
const val TABLE_RECORD = "db_record"
const val TABLE_RECORD_ID = "id"
const val TABLE_RECORD_TYPE_ID = "type_id"
const val TABLE_RECORD_ASSET_ID = "asset_id"
const val TABLE_RECORD_INTO_ASSET_ID = "into_asset_id"
const val TABLE_RECORD_BOOKS_ID = "books_id"
const val TABLE_RECORD_AMOUNT = "amount"
const val TABLE_RECORD_FINAL_AMOUNT = "final_amount"
const val TABLE_RECORD_CONCESSIONS = "concessions"
const val TABLE_RECORD_CHARGE = "charge"
const val TABLE_RECORD_REMARK = "remark"
const val TABLE_RECORD_REIMBURSABLE = "reimbursable"
const val TABLE_RECORD_RECORD_TIME = "record_time"

/* 记录关联关系表 */
const val TABLE_RECORD_RELATED = "db_record_with_related"
const val TABLE_RECORD_RELATED_ID = "id"
const val TABLE_RECORD_RELATED_RECORD_ID = "record_id"
const val TABLE_RECORD_RELATED_RELATED_RECORD_ID = "related_record_id"

/* 记录关联关系表 */
const val TABLE_IMAGE_RELATED = "db_image_with_related"
const val TABLE_IMAGE_ID = "id"
const val TABLE_IMAGE_RELATED_RECORD_ID = "record_id"
const val TABLE_IMAGE_PATH = "image_path"
const val TABLE_IMAGE_BYTES = "image_bytes"

/* 标签 */
const val TABLE_TAG = "db_tag"
const val TABLE_TAG_ID = "id"
const val TABLE_TAG_NAME = "name"
const val TABLE_TAG_BOOKS_ID = "books_id"
const val TABLE_TAG_INVISIBLE = "invisible"

/* 标签关联关系 */
const val TABLE_TAG_RELATED = "db_tag_with_record"
const val TABLE_TAG_RELATED_ID = "id"
const val TABLE_TAG_RELATED_RECORD_ID = "record_id"
const val TABLE_TAG_RELATED_TAG_ID = "tag_id"

/* 类型 */
const val TABLE_TYPE = "db_type"
const val TABLE_TYPE_ID = "id"
const val TABLE_TYPE_PARENT_ID = "parent_id"
const val TABLE_TYPE_NAME = "name"
const val TABLE_TYPE_ICON_NAME = "icon_name"
const val TABLE_TYPE_TYPE_LEVEL = "type_level"
const val TABLE_TYPE_TYPE_CATEGORY = "type_category"
const val TABLE_TYPE_PROTECTED = "protected"
const val TABLE_TYPE_SORT = "sort"
