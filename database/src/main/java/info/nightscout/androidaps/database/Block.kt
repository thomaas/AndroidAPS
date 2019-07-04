package info.nightscout.androidaps.database

import java.util.concurrent.TimeUnit

data class Block(var duration : Long, var amount : Double)

fun List<Block>.checkSanityForInsert() : Boolean {
    var sum = 0L
    forEach { sum += it.duration }
    return sum == TimeUnit.DAYS.toMillis(1)
}