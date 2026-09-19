package tw.nekomimi.nekogram.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;

/**
 * EditedMessageStorage
 *
 * Manages local persistence of edited message revisions for NasiGram Ghost Mode.
 * Operations execute asynchronously on a dedicated storage queue without blocking the UI thread.
 */
public class EditedMessageStorage {

    private static volatile EditedMessageStorage Instance;
    private final DispatchQueue storageQueue;
    private final DatabaseHelper dbHelper;

    public static class EditRecord {
        public long dialogId;
        public int messageId;
        public int date;
        public String text;
        public String mediaType;
        public String caption;

        public EditRecord(long dialogId, int messageId, int date, String text, String mediaType, String caption) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.date = date;
            this.text = text;
            this.mediaType = mediaType;
            this.caption = caption;
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "edited_messages.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS edit_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "dialog_id INTEGER, " +
                    "message_id INTEGER, " +
                    "edit_date INTEGER, " +
                    "text TEXT, " +
                    "media_type TEXT, " +
                    "caption TEXT" +
                    ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_edit_msg ON edit_history(dialog_id, message_id);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS edit_history;");
            onCreate(db);
        }
    }

    private EditedMessageStorage() {
        storageQueue = new DispatchQueue("editedMessageStorageQueue");
        dbHelper = new DatabaseHelper(ApplicationLoader.applicationContext);
    }

    public static EditedMessageStorage getInstance() {
        EditedMessageStorage localInstance = Instance;
        if (localInstance == null) {
            synchronized (EditedMessageStorage.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new EditedMessageStorage();
                }
            }
        }
        return localInstance;
    }

    public void saveEditSync(long dialogId, int messageId, String oldText, int oldDate, String mediaType, String caption) {
        if (TextUtils.isEmpty(oldText) && TextUtils.isEmpty(caption)) {
            return;
        }
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Check if identical entry already recorded for this message and date
            Cursor cursor = db.rawQuery("SELECT id FROM edit_history WHERE dialog_id = ? AND message_id = ? AND edit_date = ? LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(oldDate)});
            boolean exists = (cursor != null && cursor.moveToFirst());
            if (cursor != null) {
                cursor.close();
            }
            if (!exists) {
                ContentValues values = new ContentValues();
                values.put("dialog_id", dialogId);
                values.put("message_id", messageId);
                values.put("edit_date", oldDate);
                values.put("text", oldText);
                values.put("media_type", mediaType);
                values.put("caption", caption);
                db.insert("edit_history", null, values);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void saveEditAsync(long dialogId, int messageId, String oldText, int oldDate, String mediaType, String caption) {
        storageQueue.postRunnable(() -> saveEditSync(dialogId, messageId, oldText, oldDate, mediaType, caption));
    }

    public ArrayList<EditRecord> getEditHistorySync(long dialogId, int messageId) {
        ArrayList<EditRecord> list = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT dialog_id, message_id, edit_date, text, media_type, caption FROM edit_history WHERE dialog_id = ? AND message_id = ? ORDER BY edit_date ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(new EditRecord(
                            cursor.getLong(0),
                            cursor.getInt(1),
                            cursor.getInt(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5)
                    ));
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public boolean hasEditsSync(long dialogId, int messageId) {
        boolean has = false;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT 1 FROM edit_history WHERE dialog_id = ? AND message_id = ? LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                has = cursor.moveToFirst();
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return has;
    }
}
