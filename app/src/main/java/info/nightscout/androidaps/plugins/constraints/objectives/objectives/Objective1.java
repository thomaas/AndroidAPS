package info.nightscout.androidaps.plugins.constraints.objectives.objectives;

import java.util.List;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.database.BlockingAppRepository;
import info.nightscout.androidaps.interfaces.APSInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.constraints.objectives.ObjectivesPlugin;
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin;
import info.nightscout.androidaps.plugins.pump.virtual.VirtualPumpPlugin;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.plugins.general.actions.ActionsPlugin;
import info.nightscout.androidaps.utils.SP;

public class Objective1 extends Objective {


    public Objective1() {
        super("usage", R.string.objectives_usage_objective, R.string.objectives_usage_gate);
    }

    @Override
    protected void setupTasks(List<Task> tasks) {
        tasks.add(new Task(R.string.objectives_useprofileswitch) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveuseprofileswitch, false);
            }
        });
        tasks.add(new Task(R.string.objectives_usedisconnectpump) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveusedisconnect, false);
            }
        }.hint(new Hint(R.string.disconnectpump_hint)));
        tasks.add(new Task(R.string.objectives_usereconnectpump) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveusereconnect, false);
            }
        }.hint(new Hint(R.string.disconnectpump_hint)));
        tasks.add(new Task(R.string.objectives_usetemptarget) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveusetemptarget, false);
            }
        }.hint(new Hint(R.string.usetemptarget_hint)));
        tasks.add(new Task(R.string.objectives_useactions) {
            @Override
            public boolean isCompleted() {
                return BlockingAppRepository.INSTANCE.getLastGlucoseValue() != null;
                return SP.getBoolean(R.string.key_objectiveuseactions, false) && ActionsPlugin.INSTANCE.isEnabled(PluginType.GENERAL) && ActionsPlugin.INSTANCE.isFragmentVisible();
            }
        }.hint(new Hint(R.string.useaction_hint)));
        tasks.add(new Task(R.string.objectives_useloop) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveuseloop, false);
            }
        }.hint(new Hint(R.string.useaction_hint)));
        tasks.add(new Task(R.string.objectives_usescale) {
            @Override
            public boolean isCompleted() {
                return SP.getBoolean(R.string.key_objectiveusescale, false);
            }
        }.hint(new Hint(R.string.usescale_hint)));
    }
}
