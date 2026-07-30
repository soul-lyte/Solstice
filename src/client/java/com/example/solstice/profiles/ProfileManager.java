package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the built-in and saved-custom profiles for each {@link ProfileCategory}
 * and exposes the list for the Profiles tab.
 *
 * <p>"Selected" is deliberately not a separately-persisted flag - it's derived by
 * checking which profile's {@link Profile#matchesCurrentState()} is true right now.
 * That avoids a selection marker ever going stale relative to what's actually
 * applied (the same class of bug the old chunk-retention code was bitten by).</p>
 *
 * <p>Custom profiles are a real list per category, not a single overwritable
 * slot - saving a new one appends "Custom N" rather than replacing whatever
 * was saved before. Persisted as a count key ({@code
 * profiles.<category>.custom.count}) plus each entry's own indexed keys (see
 * {@link CustomVisualProfile}/{@link CustomPerformanceProfile}).</p>
 */
public final class ProfileManager {

    private static final ProfileManager INSTANCE = new ProfileManager();

    private static final String VISUAL_COUNT_KEY = "profiles.visual.custom.count";
    private static final String PERFORMANCE_COUNT_KEY = "profiles.performance.custom.count";

    private final VisualPvpProfile visualPvp = new VisualPvpProfile();
    private final PerformanceBalancedProfile performanceBalanced = new PerformanceBalancedProfile();
    private final PerformanceLiteProfile performanceLite = new PerformanceLiteProfile();
    private final PerformanceAggressiveProfile performanceAggressive = new PerformanceAggressiveProfile();
    private final List<CustomVisualProfile> customVisualProfiles = new ArrayList<>();
    private final List<CustomPerformanceProfile> customPerformanceProfiles = new ArrayList<>();

    private ProfileManager() {
        ConfigManager cfg = ConfigManager.getInstance();
        int visualCount = cfg.getInt(VISUAL_COUNT_KEY, 0);
        for (int i = 0; i < visualCount; i++) {
            customVisualProfiles.add(new CustomVisualProfile(i));
        }
        int performanceCount = cfg.getInt(PERFORMANCE_COUNT_KEY, 0);
        for (int i = 0; i < performanceCount; i++) {
            customPerformanceProfiles.add(new CustomPerformanceProfile(i));
        }
    }

    public static ProfileManager getInstance() {
        return INSTANCE;
    }

    /** Built-in profile(s) plus every saved Custom profile for a category, in display order. */
    public List<Profile> getProfiles(ProfileCategory category) {
        List<Profile> list = new ArrayList<>();
        if (category == ProfileCategory.VISUAL) {
            list.add(visualPvp);
            list.addAll(customVisualProfiles);
        } else {
            list.add(performanceLite);
            list.add(performanceBalanced);
            list.add(performanceAggressive);
            list.addAll(customPerformanceProfiles);
        }
        return list;
    }

    /** The profile currently matching live settings for this category, or null if nothing matches (unsaved drift). */
    public Profile getActiveProfile(ProfileCategory category) {
        for (Profile profile : getProfiles(category)) {
            if (profile.matchesCurrentState()) {
                return profile;
            }
        }
        return null;
    }

    /** Saves the current live settings as a new Custom profile for this category ("Custom N", never overwriting an existing one). */
    public void saveCustom(ProfileCategory category) {
        ConfigManager cfg = ConfigManager.getInstance();
        if (category == ProfileCategory.VISUAL) {
            CustomVisualProfile profile = new CustomVisualProfile(customVisualProfiles.size());
            profile.captureAndSave();
            customVisualProfiles.add(profile);
            cfg.set(VISUAL_COUNT_KEY, customVisualProfiles.size());
        } else {
            CustomPerformanceProfile profile = new CustomPerformanceProfile(customPerformanceProfiles.size());
            profile.captureAndSave();
            customPerformanceProfiles.add(profile);
            cfg.set(PERFORMANCE_COUNT_KEY, customPerformanceProfiles.size());
        }
    }
}
