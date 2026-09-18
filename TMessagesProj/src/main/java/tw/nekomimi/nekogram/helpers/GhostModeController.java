package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * GhostModeController
 *
 * Central architectural controller for NasiGram Ghost Mode features.
 * Manages configuration persistence via the project's existing 'nekoconfig' SharedPreferences
 * and provides decision methods for Telegram core subsystems (MessagesController, StoriesController, etc.).
 *
 * Telegram API & Protocol Limitations:
 * 1. Hide Typing & Recording: Fully functional client-side. Preventing TL_messages_setTyping suppresses all indicators.
 * 2. Hide Story Views: Fully functional client-side. Omitting TL_stories_readStories prevents server confirmation.
 * 3. Hide Read: Functional for incoming messages and media contents when browsing. Note: sending a new reply in a chat
 *    causes the Telegram MTProto server to automatically advance the read boundary on the server side.
 * 4. Hide Online & Freeze Last Seen: Client-side suppression of TL_account.updateStatus(offline=false/true).
 *    While active explicit status broadcasts are blocked, certain other MTProto RPC calls (like sending a message)
 *    may still cause the Telegram server to observe network connectivity.
 * 5. Save Deleted Messages: Client-side retention of incoming messages before deletion across private chats, groups, channels, bots.
 * 6. Save Edited Messages: Client-side retention of previous versions of messages before applying updates.
 * 7. Save Self-Destruct Media: Bypasses TTL-based file deletion for self-destructing photos/videos via NekoConfig.shouldNOTTrustMe.
 *
 * INDEPENDENCE MODEL:
 * - Ghost Mode (master toggle) controls ONLY its own privacy sub-features (hide read/typing/recording/online/lastSeen/stories).
 * - Save Deleted Messages, Save Edited Messages, and Save Self-Destruct Media are FULLY INDEPENDENT.
 *   They have their own preferences and work regardless of Ghost Mode state.
 */
public class GhostModeController {

    private static final String PREF_GHOST_MODE = "ghost_mode";
    private static final String PREF_HIDE_READ = "ghost_hide_read";
    private static final String PREF_HIDE_TYPING = "ghost_hide_typing";
    private static final String PREF_HIDE_RECORDING = "ghost_hide_recording";
    private static final String PREF_HIDE_ONLINE = "ghost_hide_online";
    private static final String PREF_FREEZE_LAST_SEEN = "ghost_freeze_last_seen";
    private static final String PREF_HIDE_STORY_VIEWS = "ghost_hide_story_views";
    private static final String PREF_SAVE_DELETED_MESSAGES = "ghost_save_deleted";
    private static final String PREF_SAVE_EDITED_MESSAGES = "ghost_save_edited";
    // saveSelfDestructMedia reuses NekoConfig's "shouldNOTTrustMe" key so FileLoader
    // automatically picks it up without any additional hook.
    private static final String PREF_SAVE_SELF_DESTRUCT_MEDIA = "shouldNOTTrustMe";

    private static volatile boolean initialized = false;

    private static boolean ghostMode;
    private static boolean hideRead;
    private static boolean hideTyping;
    private static boolean hideRecording;
    private static boolean hideOnline;
    private static boolean freezeLastSeen;
    private static boolean hideStoryViews;
    private static boolean saveDeletedMessages;
    private static boolean saveEditedMessages;
    private static boolean saveSelfDestructMedia;

    private static final Object lock = new Object();

    private static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nekoconfig", Activity.MODE_PRIVATE);
    }

    private static void checkInit() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    SharedPreferences prefs = getPreferences();
                    ghostMode = prefs.getBoolean(PREF_GHOST_MODE, false);
                    hideRead = prefs.getBoolean(PREF_HIDE_READ, false);
                    hideTyping = prefs.getBoolean(PREF_HIDE_TYPING, false);
                    hideRecording = prefs.getBoolean(PREF_HIDE_RECORDING, false);
                    hideOnline = prefs.getBoolean(PREF_HIDE_ONLINE, false);
                    freezeLastSeen = prefs.getBoolean(PREF_FREEZE_LAST_SEEN, false);
                    hideStoryViews = prefs.getBoolean(PREF_HIDE_STORY_VIEWS, false);
                    saveDeletedMessages = prefs.getBoolean(PREF_SAVE_DELETED_MESSAGES, false);
                    saveEditedMessages = prefs.getBoolean(PREF_SAVE_EDITED_MESSAGES, false);
                    // Read from NekoConfig's shared pref key so the two stay in sync
                    saveSelfDestructMedia = prefs.getBoolean(PREF_SAVE_SELF_DESTRUCT_MEDIA, false);
                    initialized = true;
                }
            }
        }
    }

    // --- Master Toggle (controls ONLY Ghost Mode privacy sub-features) ---

    public static boolean isEnabled() {
        checkInit();
        return ghostMode;
    }

    public static void setGhostMode(boolean enabled) {
        checkInit();
        ghostMode = enabled;
        getPreferences().edit().putBoolean(PREF_GHOST_MODE, enabled).apply();
    }

    public static void toggleGhostMode() {
        setGhostMode(!isEnabled());
    }

    // --- Ghost Mode Sub-Preferences ---

    public static boolean isHideReadEnabled() {
        checkInit();
        return hideRead;
    }

    public static void setHideRead(boolean enabled) {
        checkInit();
        hideRead = enabled;
        getPreferences().edit().putBoolean(PREF_HIDE_READ, enabled).apply();
    }

    public static void toggleHideRead() {
        setHideRead(!isHideReadEnabled());
    }

    public static boolean isHideTypingEnabled() {
        checkInit();
        return hideTyping;
    }

    public static void setHideTyping(boolean enabled) {
        checkInit();
        hideTyping = enabled;
        getPreferences().edit().putBoolean(PREF_HIDE_TYPING, enabled).apply();
    }

    public static void toggleHideTyping() {
        setHideTyping(!isHideTypingEnabled());
    }

    public static boolean isHideRecordingEnabled() {
        checkInit();
        return hideRecording;
    }

    public static void setHideRecording(boolean enabled) {
        checkInit();
        hideRecording = enabled;
        getPreferences().edit().putBoolean(PREF_HIDE_RECORDING, enabled).apply();
    }

    public static void toggleHideRecording() {
        setHideRecording(!isHideRecordingEnabled());
    }

    public static boolean isHideOnlineEnabled() {
        checkInit();
        return hideOnline;
    }

    public static void setHideOnline(boolean enabled) {
        checkInit();
        hideOnline = enabled;
        getPreferences().edit().putBoolean(PREF_HIDE_ONLINE, enabled).apply();
    }

    public static void toggleHideOnline() {
        setHideOnline(!isHideOnlineEnabled());
    }

    public static boolean isFreezeLastSeenEnabled() {
        checkInit();
        return freezeLastSeen;
    }

    public static void setFreezeLastSeen(boolean enabled) {
        checkInit();
        freezeLastSeen = enabled;
        getPreferences().edit().putBoolean(PREF_FREEZE_LAST_SEEN, enabled).apply();
    }

    public static void toggleFreezeLastSeen() {
        setFreezeLastSeen(!isFreezeLastSeenEnabled());
    }

    public static boolean isHideStoryViewsEnabled() {
        checkInit();
        return hideStoryViews;
    }

    public static void setHideStoryViews(boolean enabled) {
        checkInit();
        hideStoryViews = enabled;
        getPreferences().edit().putBoolean(PREF_HIDE_STORY_VIEWS, enabled).apply();
    }

    public static void toggleHideStoryViews() {
        setHideStoryViews(!isHideStoryViewsEnabled());
    }

    // --- Independent Message Tracking Preferences ---
    // These are NOT gated by Ghost Mode. Each has its own independent on/off switch.

    public static boolean isSaveDeletedMessagesEnabled() {
        checkInit();
        return saveDeletedMessages;
    }

    public static void setSaveDeletedMessages(boolean enabled) {
        checkInit();
        saveDeletedMessages = enabled;
        getPreferences().edit().putBoolean(PREF_SAVE_DELETED_MESSAGES, enabled).apply();
    }

    public static void toggleSaveDeletedMessages() {
        setSaveDeletedMessages(!isSaveDeletedMessagesEnabled());
    }

    public static boolean isSaveEditedMessagesEnabled() {
        checkInit();
        return saveEditedMessages;
    }

    public static void setSaveEditedMessages(boolean enabled) {
        checkInit();
        saveEditedMessages = enabled;
        getPreferences().edit().putBoolean(PREF_SAVE_EDITED_MESSAGES, enabled).apply();
    }

    public static void toggleSaveEditedMessages() {
        setSaveEditedMessages(!isSaveEditedMessagesEnabled());
    }

    public static boolean isSaveSelfDestructMediaEnabled() {
        checkInit();
        return saveSelfDestructMedia;
    }

    /**
     * Enables/disables saving of self-destruct photos and videos.
     *
     * Implementation note: This writes to the "shouldNOTTrustMe" key in "nekoconfig" SharedPreferences.
     * FileLoader reads NekoConfig.shouldNOTTrustMe (loaded from the same key) and uses it to decide
     * whether to treat TTL-marked media as regular cached files instead of self-destructing ones.
     * By syncing to the same key, we avoid any additional hook in FileLoader.
     */
    public static void setSaveSelfDestructMedia(boolean enabled) {
        checkInit();
        saveSelfDestructMedia = enabled;
        // Write to NekoConfig's own pref key so NekoConfig.shouldNOTTrustMe stays in sync at next load.
        getPreferences().edit().putBoolean(PREF_SAVE_SELF_DESTRUCT_MEDIA, enabled).apply();
        // Also update NekoConfig's in-memory field directly so the change takes effect immediately
        // without requiring an app restart.
        tw.nekomimi.nekogram.NekoConfig.shouldNOTTrustMe = enabled;
    }

    public static void toggleSaveSelfDestructMedia() {
        setSaveSelfDestructMedia(!isSaveSelfDestructMediaEnabled());
    }

    // --- Decision Methods used across the codebase ---
    // Ghost Mode sub-features: REQUIRE master toggle to be ON.

    public static boolean shouldHideRead() {
        return isEnabled() && isHideReadEnabled();
    }

    public static boolean shouldHideTyping() {
        return isEnabled() && isHideTypingEnabled();
    }

    public static boolean shouldHideRecording() {
        return isEnabled() && isHideRecordingEnabled();
    }

    public static boolean shouldHideOnline() {
        return isEnabled() && isHideOnlineEnabled();
    }

    public static boolean shouldFreezeLastSeen() {
        return isEnabled() && isFreezeLastSeenEnabled();
    }

    public static boolean shouldHideStoryViews() {
        return isEnabled() && isHideStoryViewsEnabled();
    }

    // Message Tracking features: INDEPENDENT from Ghost Mode master toggle.

    /** Returns true if deleted messages should be intercepted and stored locally. */
    public static boolean shouldSaveDeletedMessages() {
        return isSaveDeletedMessagesEnabled();
    }

    /** Returns true if previous message versions should be captured before edits are applied. */
    public static boolean shouldSaveEditedMessages() {
        return isSaveEditedMessagesEnabled();
    }

    /** Returns true if self-destruct media (TTL photos/videos) should bypass deletion and be saved. */
    public static boolean shouldSaveSelfDestructMedia() {
        return isSaveSelfDestructMediaEnabled();
    }

    public static void onMessageSent(int currentAccount) {
        if (shouldHideOnline()) {
            org.telegram.messenger.MessagesController.getInstance(currentAccount).markOfflineAfterAction();
        }
    }
}
