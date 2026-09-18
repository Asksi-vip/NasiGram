package tw.nekomimi.nekogram.settings;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class NekoSettingsActivity extends BaseNekoSettingsActivity {

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Blank page as requested by the user
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.NekoSettings);
    }
}
