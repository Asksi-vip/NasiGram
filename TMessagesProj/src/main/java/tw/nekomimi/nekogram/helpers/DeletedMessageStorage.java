package tw.nekomimi.nekogram.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * DeletedMessageStorage
 *
 * Manages local persistence and lookup of deleted messages for NasiGram Ghost Mode.
 * Operations execute asynchronously on a dedicated storage queue without blocking the UI thread.
 */
public class DeletedMessageStorage {

    private static volatile DeletedMessageStorage Instance;
    private final DispatchQueue storageQueue;
    private final DatabaseHelper dbHelper;
    private final Set<String> fastDeletedLookup = new HashSet<>();

    public static class DeletedRecord {
        public long dialogId;
        public int messageId;
        public long fromId;
        public int date;
        public int deleteDate;
        public String text;
        public String caption;
        public String mediaType;
        public String mediaPath;

        public DeletedRecord(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.fromId = fromId;
            this.date = date;
            this.deleteDate = deleteDate;
            this.text = text;
            this.caption = caption;
            this.mediaType = mediaType;
            this.mediaPath = mediaPath;
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "deleted_messages.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_messages (" +
                    "dialog_id INTEGER, " +
                    "message_id INTEGER, " +
                    "from_id INTEGER, " +
                    "date INTEGER, " +
                    "delete_date INTEGER, " +
                    "text TEXT, " +
                    "caption TEXT, " +
                    "media_type TEXT, " +
                    "media_path TEXT, " +
                    "PRIMARY KEY(dialog_id, message_id)" +
                    ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_del_dialog ON deleted_messages(dialog_id);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS deleted_messages;");
            onCreate(db);
        }
    }

    private DeletedMessageStorage() {
        storageQueue = new DispatchQueue("deletedMessageStorageQueue");
        dbHelper = new DatabaseHelper(ApplicationLoader.applicationContext);
        loadFastCache();
    }

    private void loadFastCache() {
        storageQueue.postRunnable(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT dialog_id, message_id FROM deleted_messages", null);
                if (cursor != null) {
                    synchronized (fastDeletedLookup) {
                        while (cursor.moveToNext()) {
                            fastDeletedLookup.add(cursor.getLong(0) + "_" + cursor.getInt(1));
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static DeletedMessageStorage getInstance() {
        DeletedMessageStorage localInstance = Instance;
        if (localInstance == null) {
            synchronized (DeletedMessageStorage.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new DeletedMessageStorage();
                }
            }
        }
        return localInstance;
    }

    public void saveDeletedMessageAsync(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath) {
        synchronized (fastDeletedLookup) {
            fastDeletedLookup.add(dialogId + "_" + messageId);
        }
        storageQueue.postRunnable(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("dialog_id", dialogId);
                values.put("message_id", messageId);
                values.put("from_id", fromId);
                values.put("date", date);
                values.put("delete_date", deleteDate);
                values.put("text", text);
                values.put("caption", caption);
                values.put("media_type", mediaType);
                values.put("media_path", mediaPath);
                db.insertWithOnConflict("deleted_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public boolean isDeletedSync(long dialogId, int messageId) {
        synchronized (fastDeletedLookup) {
            if (fastDeletedLookup.contains(dialogId + "_" + messageId)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<DeletedRecord> getDeletedMessagesForDialogSync(long dialogId) {
        ArrayList<DeletedRecord> list = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT dialog_id, message_id, from_id, date, delete_date, text, caption, media_type, media_path FROM deleted_messages WHERE dialog_id = ? ORDER BY date DESC",
                    new String[]{String.valueOf(dialogId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(new DeletedRecord(
                            cursor.getLong(0),
                            cursor.getInt(1),
                            cursor.getLong(2),
                            cursor.getInt(3),
                            cursor.getInt(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8)
                    ));
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }
}
