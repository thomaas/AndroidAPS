package info.nightscout.androidaps.plugins.source;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import info.nightscout.androidaps.db.DatabaseHelper;

@DatabaseTable(tableName = DatabaseHelper.DATABASE_LIBRE2_RAW_Values)
public class Libre2RawValue {

    @DatabaseField(generatedId = true)
    public long id;

    @DatabaseField
    public long timestamp;

    @DatabaseField
    public double glucose;
}
