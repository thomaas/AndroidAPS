package info.nightscout.androidaps.plugins.general.open_humans

import info.nightscout.androidaps.database.Block
import info.nightscout.androidaps.database.embedments.InsulinConfiguration
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.APSResultLink
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.interfaces.DBEntryWithDuration
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime
import info.nightscout.androidaps.database.interfaces.DBEntryWithTimeAndDuration
import info.nightscout.androidaps.database.interfaces.TraceableDBEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class UploadData(
        val timestamp: Long,
        val uploadCounter: Int,
        val fileFormatVersion: Int,

        val versionName: String,
        val versionCode: Int,
        val databaseVersion: Int,
        val hasGitInfo: Boolean,
        val commitHash: String?,
        val customRemote: Boolean,
        val applicationId: UUID,

        val brand: String,
        val device: String,
        val manufacturer: String,
        val model: String,
        val product: String,

        val height: Int,
        val width: Int,
        val density: Float,
        val scaledDensity: Float,
        val xDpi: Float,
        val yDpi: Float,

        val apsResultLinks: List<APSResultLink>,
        val mealLinks: List<MealLink>,
        val multiwaveBolusLinks: List<MultiwaveBolusLink>,
        val apsResults: List<APSResult>,
        val boluses: List<Bolus>,
        val bolusCalculatorResults: List<BolusCalculatorResult>,
        val carbs: List<Carbs>,
        val effectiveProfileSwitches: List<EffectiveProfileSwitch>,
        val extendedBoluses: List<ExtendedBolus>,
        val glucoseValues: List<GlucoseValue>,
        val profileSwitches: List<ProfileSwitch>,
        val temporaryBasals: List<TemporaryBasal>,
        val temporaryTargets: List<TemporaryTarget>,
        val therapyEvents: List<TherapyEvent>,
        val totalDailyDoses: List<TotalDailyDose>,
        val versionChanges: List<VersionChange>,
        val preferenceChanges: List<PreferenceChange>
) {
    val lowestTimestamp by lazy {
        listOfNotNull(
                apsResults.map { it.timestamp }.min(),
                boluses.map { it.timestamp }.min(),
                bolusCalculatorResults.map { it.timestamp }.min(),
                carbs.map { it.timestamp }.min(),
                effectiveProfileSwitches.map { it.timestamp }.min(),
                extendedBoluses.map { it.timestamp }.min(),
                glucoseValues.map { it.timestamp }.min(),
                profileSwitches.map { it.timestamp }.min(),
                temporaryBasals.map { it.timestamp }.min(),
                temporaryTargets.map { it.timestamp }.min(),
                therapyEvents.map { it.timestamp }.min(),
                totalDailyDoses.map { it.timestamp }.min(),
                versionChanges.map { it.timestamp }.min(),
                preferenceChanges.map { it.timestamp }.min()).min()
    }

    val highestTimestamp by lazy {
        listOfNotNull(
                apsResults.map { it.timestamp }.max(),
                boluses.map { it.timestamp }.max(),
                bolusCalculatorResults.map { it.timestamp }.max(),
                carbs.map { it.timestamp }.max(),
                effectiveProfileSwitches.map { it.timestamp }.max(),
                extendedBoluses.map { it.timestamp }.max(),
                glucoseValues.map { it.timestamp }.max(),
                profileSwitches.map { it.timestamp }.max(),
                temporaryBasals.map { it.timestamp }.max(),
                temporaryTargets.map { it.timestamp }.max(),
                therapyEvents.map { it.timestamp }.max(),
                totalDailyDoses.map { it.timestamp }.max(),
                versionChanges.map { it.timestamp }.max(),
                preferenceChanges.map { it.timestamp }.max()).max()
    }

    val tags by lazy {
        mutableListOf<String>().run {
            if (apsResultLinks.isNotEmpty()) add("APSResultLinks")
            if (mealLinks.isNotEmpty()) add("MealLinks")
            if (multiwaveBolusLinks.isNotEmpty()) add("MultiwaveBolusLinks")
            if (apsResults.isNotEmpty()) add("APSResults")
            if (boluses.isNotEmpty()) add("Boluses")
            if (bolusCalculatorResults.isNotEmpty()) add("BolusCalculatorResults")
            if (carbs.isNotEmpty()) add("Carbs")
            if (effectiveProfileSwitches.isNotEmpty()) add("EffectiveProfileSwitches")
            if (extendedBoluses.isNotEmpty()) add("ExtendedBolus")
            if (glucoseValues.isNotEmpty()) add("GlucoseValues")
            if (profileSwitches.isNotEmpty()) add("ProfileSwitches")
            if (temporaryBasals.isNotEmpty()) add("TemporaryBasals")
            if (temporaryTargets.isNotEmpty()) add("TemporaryTargets")
            if (therapyEvents.isNotEmpty()) add("TherapyEvents")
            if (totalDailyDoses.isNotEmpty()) add("TotalDailyDoses")
            if (versionChanges.isNotEmpty()) add("VersionChanges")
            if (preferenceChanges.isNotEmpty()) add("PreferenceChanges")
            add("ApplicationInfo")
            add("DeviceInfo")
            add("DisplayInfo")
            add("UploadInfo")
            toList()
        }
    }

    val fileName by lazy { "upload-num${String.format("%05d", uploadCounter)}-ver$fileFormatVersion-dev$applicationId.zip" }

    val zip by lazy {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        if (apsResultLinks.isNotEmpty()) zos.writeFile("apsResultLinks.json", apsResultLinks.map { it.serializeToJSON() }.toByteArray())
        if (mealLinks.isNotEmpty()) zos.writeFile("mealLinks.json", mealLinks.map { it.serializeToJSON() }.toByteArray())
        if (multiwaveBolusLinks.isNotEmpty()) zos.writeFile("multiwaveBolusLinks.json", multiwaveBolusLinks.map { it.serializeToJSON() }.toByteArray())
        if (apsResults.isNotEmpty()) zos.writeFile("apsResults.json", apsResults.map { it.serializeToJSON() }.toByteArray())
        if (boluses.isNotEmpty()) zos.writeFile("boluses.json", boluses.map { it.serializeToJSON() }.toByteArray())
        if (bolusCalculatorResults.isNotEmpty()) zos.writeFile("bolusCalculatorResults.json", bolusCalculatorResults.map { it.serializeToJSON() }.toByteArray())
        if (carbs.isNotEmpty()) zos.writeFile("carbs.json", carbs.map { it.serializeToJSON() }.toByteArray())
        if (effectiveProfileSwitches.isNotEmpty()) zos.writeFile("effectiveProfileSwitches.json", effectiveProfileSwitches.map { it.serializeToJSON() }.toByteArray())
        if (extendedBoluses.isNotEmpty()) zos.writeFile("extendedBoluses.json", extendedBoluses.map { it.serializeToJSON() }.toByteArray())
        if (glucoseValues.isNotEmpty()) zos.writeFile("glucoseValues.json", glucoseValues.map { it.serializeToJSON() }.toByteArray())
        if (profileSwitches.isNotEmpty()) zos.writeFile("profileSwitches.json", profileSwitches.map { it.serializeToJSON() }.toByteArray())
        if (temporaryBasals.isNotEmpty()) zos.writeFile("temporaryBasals.json", temporaryBasals.map { it.serializeToJSON() }.toByteArray())
        if (temporaryTargets.isNotEmpty()) zos.writeFile("temporaryTargets.json", temporaryTargets.map { it.serializeToJSON() }.toByteArray())
        if (therapyEvents.isNotEmpty()) zos.writeFile("therapyEvents.json", therapyEvents.map { it.serializeToJSON() }.toByteArray())
        if (totalDailyDoses.isNotEmpty()) zos.writeFile("totalDailyDoses.json", totalDailyDoses.map { it.serializeToJSON() }.toByteArray())
        if (versionChanges.isNotEmpty()) zos.writeFile("versionChanges.json", versionChanges.map { it.serializeToJSON() }.toByteArray())
        if (preferenceChanges.isNotEmpty()) zos.writeFile("preferenceChanges.json", preferenceChanges.map { it.serializeToJSON() }.toByteArray())
        zos.writeFile("applicationInfo.json", serializeApplicationInfoToJSON().toString().toByteArray())
        zos.writeFile("uploadInfo.json", serializeUploadInfoToJSON().toString().toByteArray())
        zos.writeFile("deviceInfo.json", serializeDeviceInfoToJSON().toString().toByteArray())
        zos.writeFile("displayInfo.json", serializeDisplayInfoToJSON().toString().toByteArray())
        zos.close()
        baos.toByteArray()
    }

    val zipMd5 by lazy { zip.md5() }

    private fun ZipOutputStream.writeFile(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun serializeDisplayInfoToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("height", brand)
        jsonObject.put("width", height)
        jsonObject.put("density", density)
        jsonObject.put("scaledDensity", scaledDensity)
        jsonObject.put("xDpi", xDpi)
        jsonObject.put("yDpi", yDpi)
        return jsonObject
    }

    private fun serializeDeviceInfoToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("brand", brand)
        jsonObject.put("device", device)
        jsonObject.put("manufacturer", manufacturer)
        jsonObject.put("model", model)
        jsonObject.put("product", product)
        return jsonObject
    }

    private fun serializeUploadInfoToJSON(): JSONObject {
        val jsonObject = JSONObject()
        if (lowestTimestamp != null) jsonObject.put("from", lowestTimestamp!!)
        if (highestTimestamp != null) jsonObject.put("to", highestTimestamp!!)
        jsonObject.put("fileVersion", fileFormatVersion)
        jsonObject.put("counter", uploadCounter)
        jsonObject.put("timestamp", timestamp)
        return jsonObject
    }

    private fun serializeApplicationInfoToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("versionCode", versionCode)
        jsonObject.put("versionName", versionName)
        jsonObject.put("databaseVersion", databaseVersion)
        jsonObject.put("hasGitInfo", hasGitInfo)
        if (commitHash != null) jsonObject.put("commitHash", commitHash)
        jsonObject.put("customRemote", customRemote)
        jsonObject.put("applicationId", applicationId.toString())
        return jsonObject
    }

    private fun PreferenceChange.serializeToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("timestamp", timestamp)
        jsonObject.put("utcOffset", utcOffset)
        jsonObject.put("key", key)
        jsonObject.put("value", value)
        return jsonObject
    }

    private fun VersionChange.serializeToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("timestamp", timestamp)
        jsonObject.put("utcOffset", utcOffset)
        jsonObject.put("versionCode", versionCode)
        jsonObject.put("versionName", versionName)
        jsonObject.put("customGitRemote", gitRemote != "https://github.com/MilosKozak/AndroidAPS.git")
        jsonObject.put("commitHash", commitHash)
        return jsonObject
    }

    private fun List<JSONObject>.toByteArray(): ByteArray {
        val jsonArray = JSONArray()
        forEach { jsonArray.put(it) }
        return jsonArray.toString().toByteArray()
    }

    private fun APSResultLink.serializeToJSON() = serializeDBEntry().let {
        it.put("apsResultId", apsResultId)
        it.put("smbId", smbId)
        it.put("tbrId", tbrId)
    }

    private fun MealLink.serializeToJSON() = serializeDBEntry().let {
        it.put("bolusId", bolusId)
        it.put("carbsId", carbsId)
        it.put("bolusCalculatorResultId", bolusCalcResultId)
        it.put("superbolusTempBasalId", superbolusTempBasalId)
        it.put("noteId", noteId)
    }

    private fun MultiwaveBolusLink.serializeToJSON() = serializeDBEntry().let {
        it.put("bolusId", bolusId)
        it.put("extendedBolusId", extendedBolusId)
    }

    private fun APSResult.serializeToJSON() = serializeDBEntry().let {
        serializeTime(it)
        it.put("algorithm", algorithm.name)
        it.put("glucoseStatus", glucoseStatusJson.parseJSON())
        it.put("currentTemp", currentTempJson.parseJSON())
        it.put("iobData", iobDataJson.parseJSON())
        it.put("profile", profileJson.parseJSON())
        it.put("autosensData", autosensDataJson.parseJSON())
        it.put("mealData", mealDataJson.parseJSON())
        it.put("isMicroBolusAllowed", isMicroBolusAllowed)
        it.put("result", resultJson.parseJSON())
    }

    private fun Bolus.serializeToJSON() = serializeDBEntry().let {
        serializeTime(it)
        it.put("amount", amount)
        it.put("type", type.name)
        it.put("isBasalInsulin", isBasalInsulin)
        it.put("insulinConfiguration", insulinConfiguration?.serializeToJSON())
    }

    private fun BolusCalculatorResult.serializeToJSON() = serializeDBEntry().let {
        serializeTime(it)
        it.put("targetBGLow", targetBGLow)
        it.put("targetBGHigh", targetBGHigh)
        it.put("isf", isf)
        it.put("ic", ic)
        it.put("bolusIOB", bolusIOB)
        it.put("wasBolusIOBUsed", wasBolusIOBUsed)
        it.put("basalIOB", basalIOB)
        it.put("wasBasalIOBUsed", wasBasalIOBUsed)
        it.put("glucoseValue", glucoseValue)
        it.put("wasGlucoseValueUsed", wasGlucoseUsed)
        it.put("glucoseDifference", glucoseDifference)
        it.put("glucoseInsulin", glucoseInsulin)
        it.put("glucoseTrend", glucoseTrend)
        it.put("wasTrendUsed", wasTrendUsed)
        it.put("trendInsulin", trendInsulin)
        it.put("cob", cob)
        it.put("wasCOBUsed", wasCOBUsed)
        it.put("cobInsulin", cobInsulin)
        it.put("carbs", carbs)
        it.put("wereCarbsUsed", wereCarbsUsed)
        it.put("carbsInsulin", carbsInsulin)
        it.put("otherCorrection", otherCorrection)
        it.put("wasSuperbolusUsed", wasSuperbolusUsed)
        it.put("superbolusInsulin", superbolusInsulin)
        it.put("wasTempTargetUsed", wasTempTargetUsed)
        it.put("totalInsulin", totalInsulin)
    }

    private fun Carbs.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("amount", amount)
    }

    private fun EffectiveProfileSwitch.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("basalBlocks", basalBlocks.serializeToJSON())
    }

    private fun ExtendedBolus.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("amount", amount)
        it.put("isEmulatingTempBasal", isEmulatingTempBasal)
    }

    private fun GlucoseValue.serializeToJSON() = serializeDBEntry().let {
        serializeTime(it)
        it.put("raw", raw)
        it.put("value", value)
        it.put("trendArrow", trendArrow.name)
        it.put("noise", noise)
        it.put("sourceSensor", sourceSensor)
    }

    private fun ProfileSwitch.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("glucoseUnit", glucoseUnit.name)
        it.put("basalBlocks", basalBlocks.serializeToJSON())
        it.put("isfBlocks", isfBlocks.serializeToJSON())
        it.put("icBlocks", icBlocks.serializeToJSON())
        val targetBlockArray = JSONArray()
        targetBlocks.map { targetBlock ->
            val jsonObject = JSONObject()
            jsonObject.put("duration", targetBlock.duration)
            jsonObject.put("lowTarget", targetBlock.lowTarget)
            jsonObject.put("highTarget", targetBlock.highTarget)
            jsonObject
        }.forEach { targetBlock -> targetBlockArray.put(targetBlock) }
        it.put("targetBlocks", targetBlockArray)
        it.put("insulinConfiguration", insulinConfiguration.serializeToJSON())
        it.put("timeshift", timeshift)
        it.put("percentage", percentage)
    }

    private fun TemporaryBasal.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("type", type.name)
        it.put("isAbsolute", isAbsolute)
        it.put("rate", rate)
    }

    private fun TemporaryTarget.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("reason", reason.name)
        it.put("target", target)
    }

    private fun TherapyEvent.serializeToJSON() = serializeDBEntry().let {
        serializeTimeAndDuration(it)
        it.put("type", type.name)
        it.put("amount", amount)
    }

    private fun TotalDailyDose.serializeToJSON() = serializeDBEntry().let {
        serializeTime(it)
        it.put("basalAmount", basalAmount)
        it.put("bolusAmount", bolusAmount)
        it.put("totalAmount", totalAmount)
    }

    private fun List<Block>.serializeToJSON(): JSONArray {
        val jsonArray = JSONArray()
        forEach { jsonArray.put(it.serializeToJSON()) }
        return jsonArray
    }

    private fun Block.serializeToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("duration", duration)
        jsonObject.put("amount", amount)
        return jsonObject
    }

    private fun String?.parseJSON() = when {
        this == null -> null
        this.startsWith("[") -> JSONArray(this)
        this.startsWith("{") -> JSONObject(this)
        else -> throw IllegalArgumentException("Cannot parse JSON")
    }

    private fun TraceableDBEntry.serializeDBEntry(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("version", version)
        jsonObject.put("dateCreated", dateCreated)
        jsonObject.put("isValid", isValid)
        jsonObject.put("referenceId", referenceId)
        jsonObject.put("interfaceIDs", interfaceIDs.serializeToJSON())
        return jsonObject
    }

    private fun DBEntryWithTime.serializeTime(jsonObject: JSONObject) {
        jsonObject.put("timestamp", timestamp)
        jsonObject.put("utcOffset", utcOffset)
    }

    private fun DBEntryWithDuration.serializeDuration(jsonObject: JSONObject) {
        jsonObject.put("duration", duration)
    }

    private fun InsulinConfiguration.serializeToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("insulinEndTime", insulinEndTime)
        jsonObject.put("peak", peak)
        return jsonObject
    }

    private fun DBEntryWithTimeAndDuration.serializeTimeAndDuration(jsonObject: JSONObject) {
        serializeTime(jsonObject)
        serializeDuration(jsonObject)
    }

    private fun InterfaceIDs.serializeToJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("pumpType", pumpType?.name)
        jsonObject.put("pumpSerial", pumpSerial?.sha256())
        jsonObject.put("pumpId", pumpId)
        jsonObject.put("startId", startId)
        jsonObject.put("endId", endId)
        return jsonObject
    }

    private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

    private fun ByteArray.toHexString(): String {
        val stringBuilder = StringBuilder()
        map { it.toInt() }.forEach {
            stringBuilder.append(HEX_DIGITS[(it shr 4) and 0x0F])
            stringBuilder.append(HEX_DIGITS[it and 0x0F])
        }
        return stringBuilder.toString()
    }

    private fun String.sha256() = MessageDigest.getInstance("SHA-256").digest(toByteArray()).toHexString()

    private fun ByteArray.md5() = MessageDigest.getInstance("MD5").digest(this).toHexString()
}