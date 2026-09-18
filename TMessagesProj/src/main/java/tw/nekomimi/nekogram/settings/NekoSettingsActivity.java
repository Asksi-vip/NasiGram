package tw.nekomimi.nekogram.settings;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.GhostModeController;

public class NekoSettingsActivity extends BaseNekoSettingsActivity {

    private final int ghostModeRow = rowId++;
    private final int hideReadRow = rowId++;
    private final int hideTypingRow = rowId++;
    private final int hideRecordingRow = rowId++;
    private final int hideOnlineRow = rowId++;
    private final int freezeLastSeenRow = rowId++;
    private final int hideStoryViewsRow = rowId++;
    private final int saveDeletedRow = rowId++;
    private final int saveEditedRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean masterEnabled = GhostModeController.isEnabled();

        // ─── Ghost Mode Section ───
        items.add(UItem.asHeader(LocaleController.getString(R.string.GhostMode)));
        items.add(UItem.asCheck(ghostModeRow, LocaleController.getString(R.string.GhostMode))
                .slug("ghost_mode")
                .setChecked(masterEnabled));

        items.add(UItem.asCheck(hideReadRow, LocaleController.getString(R.string.GhostHideRead), LocaleController.getString(R.string.GhostHideReadAbout))
                .slug("ghost_hide_read")
                .setChecked(GhostModeController.isHideReadEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asCheck(hideTypingRow, LocaleController.getString(R.string.GhostHideTyping), LocaleController.getString(R.string.GhostHideTypingAbout))
                .slug("ghost_hide_typing")
                .setChecked(GhostModeController.isHideTypingEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asCheck(hideRecordingRow, LocaleController.getString(R.string.GhostHideRecording), LocaleController.getString(R.string.GhostHideRecordingAbout))
                .slug("ghost_hide_recording")
                .setChecked(GhostModeController.isHideRecordingEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asCheck(hideOnlineRow, LocaleController.getString(R.string.GhostHideOnline), LocaleController.getString(R.string.GhostHideOnlineAbout))
                .slug("ghost_hide_online")
                .setChecked(GhostModeController.isHideOnlineEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asCheck(freezeLastSeenRow, LocaleController.getString(R.string.GhostFreezeLastSeen), LocaleController.getString(R.string.GhostFreezeLastSeenAbout))
                .slug("ghost_freeze_last_seen")
                .setChecked(GhostModeController.isFreezeLastSeenEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asCheck(hideStoryViewsRow, LocaleController.getString(R.string.GhostHideStoryViews), LocaleController.getString(R.string.GhostHideStoryViewsAbout))
                .slug("ghost_hide_story_views")
                .setChecked(GhostModeController.isHideStoryViewsEnabled())
                .setEnabled(masterEnabled));

        items.add(UItem.asShadow(LocaleController.getString(R.string.GhostModeAbout)));

        // ─── Message Tracking Section (independent of Ghost Mode) ───
        items.add(UItem.asHeader(LocaleController.getString(R.string.MessageTrackingHeader)));

        items.add(UItem.asCheck(saveDeletedRow, LocaleController.getString(R.string.GhostSaveDeleted), LocaleController.getString(R.string.GhostSaveDeletedAbout))
                .slug("ghost_save_deleted")
                .setChecked(GhostModeController.isSaveDeletedMessagesEnabled()));

        items.add(UItem.asCheck(saveEditedRow, LocaleController.getString(R.string.GhostSaveEdited), LocaleController.getString(R.string.GhostSaveEditedAbout))
                .slug("ghost_save_edited")
                .setChecked(GhostModeController.isSaveEditedMessagesEnabled()));

        items.add(UItem.asShadow(LocaleController.getString(R.string.MessageTrackingAbout)));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;

        // Message Tracking items are always clickable, regardless of Ghost Mode state
        if (id == saveDeletedRow) {
            GhostModeController.toggleSaveDeletedMessages();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return;
        } else if (id == saveEditedRow) {
            GhostModeController.toggleSaveEditedMessages();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return;
        }

        // Ghost Mode items require master toggle check
        if (id == ghostModeRow) {
            GhostModeController.toggleGhostMode();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (!GhostModeController.isEnabled()) {
            return;
        } else if (id == hideReadRow) {
            GhostModeController.toggleHideRead();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == hideTypingRow) {
            GhostModeController.toggleHideTyping();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == hideRecordingRow) {
            GhostModeController.toggleHideRecording();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == hideOnlineRow) {
            GhostModeController.toggleHideOnline();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == freezeLastSeenRow) {
            GhostModeController.toggleFreezeLastSeen();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == hideStoryViewsRow) {
            GhostModeController.toggleHideStoryViews();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.NekoSettings);
    }
}
