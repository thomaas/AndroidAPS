package info.nightscout.androidaps.database.transactions

abstract class Transaction<T> {

    internal abstract fun process(): T

}