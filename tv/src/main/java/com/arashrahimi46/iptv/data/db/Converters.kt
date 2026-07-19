package com.arashrahimi46.iptv.data.db

import androidx.room.TypeConverter
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType = ContentType.valueOf(value)
}
