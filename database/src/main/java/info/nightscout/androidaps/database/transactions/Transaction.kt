package info.nightscout.androidaps.database.transactions

abstract class Transaction<T> {

    internal abstract fun run(): T

}