package info.nightscout.androidaps.plugins.general.automation.triggers;

import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.plugins.general.automation.elements.Comparator;

public abstract class Trigger {

    protected TriggerConnector connector = null;

    Trigger() {
    }

    public TriggerConnector getConnector() {
        return connector;
    }

    public abstract boolean shouldRun();


    public abstract String toJSON();

    /*package*/ abstract Trigger fromJSON(String data);

    public abstract int friendlyName();

    public abstract String friendlyDescription();

    public abstract Optional<Integer> icon();

    public abstract void executed(long time);

    public abstract Trigger duplicate();

    public static Trigger instantiate(String json) {
        try {
            return instantiate(new JSONObject(json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Trigger instantiate(JSONObject object) {
        try {
            String type = object.getString("type");
            JSONObject data = object.getJSONObject("data");
            Class clazz = Class.forName(type);
            return ((Trigger) clazz.newInstance()).fromJSON(data.toString());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void generateDialog(LinearLayout root) {
        TextView title = new TextView(root.getContext());
        title.setText(friendlyName());
        root.addView(title);
    }

    public View createView(Context context, FragmentManager fragmentManager) {
        final int padding = MainApp.dpToPx(4);

        LinearLayout root = new LinearLayout(context);
        root.setPadding(padding, padding, padding, padding);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(context);
        title.setText(friendlyName());
        root.addView(title);

        return root;
    }
}
