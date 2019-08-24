package info.nightscout.androidaps.interfaces;

import androidx.annotation.NonNull;

import info.nightscout.androidaps.data.Profile;

/**
 * Created by mike on 15.06.2016.
 */
public interface ConstraintsInterface {
    @NonNull
    default Constraint<Boolean> isLoopInvocationAllowed(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isClosedLoopAllowed(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isAutosensModeEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isAMAModeEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isSMBModeEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isUAMEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isAdvancedFilteringEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Boolean> isSuperBolusEnabled(@NonNull Constraint<Boolean> value) {
        return value;
    }
    @NonNull
    default Constraint<Double> applyBasalConstraints(@NonNull Constraint<Double> absoluteRate, @NonNull Profile profile) {
        return absoluteRate;
    }
    @NonNull
    default Constraint<Integer> applyBasalPercentConstraints(@NonNull Constraint<Integer> percentRate, @NonNull Profile profile) {
        return percentRate;
    }
    @NonNull
    default Constraint<Double>  applyBolusConstraints(@NonNull Constraint<Double>  insulin) {
        return insulin;
    }
    @NonNull
    default Constraint<Double>  applyExtendedBolusConstraints(@NonNull Constraint<Double>  insulin) {
        return insulin;
    }
    @NonNull
    default Constraint<Integer> applyCarbsConstraints(@NonNull Constraint<Integer> carbs) {
        return carbs;
    }
    @NonNull
    default Constraint<Double> applyMaxIOBConstraints(@NonNull Constraint<Double> maxIob) {
        return maxIob;
    }

}
