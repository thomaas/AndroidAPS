package info.nightscout.androidaps.database

import androidx.room.TypeConverter
import info.nightscout.androidaps.database.entities.*
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromBolusType(bolusType: Bolus.Type) = bolusType.name

    @TypeConverter
    fun toBolusType(bolusType: String) = Bolus.Type.valueOf(bolusType)

    @TypeConverter
    fun fromTrendArrow(trendArrow: GlucoseValue.TrendArrow) = trendArrow.name

    @TypeConverter
    fun toTrendArrow(trendArrow: String) = GlucoseValue.TrendArrow.valueOf(trendArrow)

    @TypeConverter
    fun fromSourceSensor(sourceSensor: GlucoseValue.SourceSensor) = sourceSensor.name

    @TypeConverter
    fun toSourceSensor(sourceSensor: String) = GlucoseValue.SourceSensor.valueOf(sourceSensor)

    @TypeConverter
    fun fromTBRType(tbrType: TemporaryBasal.Type) = tbrType.name

    @TypeConverter
    fun toTBRType(tbrType: String) = TemporaryBasal.Type.valueOf(tbrType)

    @TypeConverter
    fun fromTempTargetReason(tempTargetReason: TemporaryTarget.Reason) = tempTargetReason.name

    @TypeConverter
    fun toTempTargetReason(tempTargetReason: String) = TemporaryTarget.Reason.valueOf(tempTargetReason)

    @TypeConverter
    fun fromTherapyEventType(therapyEventType: TherapyEvent.Type) = therapyEventType.name

    @TypeConverter
    fun toTherapyEventType(therapyEventType: String) = TherapyEvent.Type.valueOf(therapyEventType)

    @TypeConverter
    fun fromGlucoseUnit(glucoseUnit: ProfileSwitch.GlucoseUnit) = glucoseUnit.name

    @TypeConverter
    fun toGlucoseUnit(glucoseUnit: String) = ProfileSwitch.GlucoseUnit.valueOf(glucoseUnit)

    @TypeConverter
    fun fromListOfBlocks(blocks: List<Block>): String {
        val jsonArray = JSONArray()
        blocks.forEach {
            val jsonObject = JSONObject()
            jsonObject.put("duration", it.duration)
            jsonObject.put("amount", it.amount)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toListOfBlocks(jsonString: String) : List<Block> {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<Block>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            list.add(Block(jsonObject.getLong("duration"), jsonObject.getDouble("amount")))
        }
        return list
    }

}