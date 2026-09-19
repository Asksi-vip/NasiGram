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
 * Operations can execute synchronously on MessagesStorage queue or asynchronously on a dedicated storage queue.
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
        public int replyToMid;
        public int editDate;
        public boolean isOut;

        public DeletedRecord(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath) {
            this(dialogId, messageId, fromId, date, deleteDate, text, caption, mediaType, mediaPath, 0, 0, false);
        }

        public DeletedRecord(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath, int replyToMid, int editDate, boolean isOut) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.fromId = fromId;
            this.date = date;
            this.deleteDate = deleteDate;
            this.text = text;
            this.caption = caption;
            this.mediaType = mediaType;
            this.mediaPath = mediaPath;
            this.replyToMid = replyToMid;
            this.editDate = editDate;
            this.isOut = isOut;
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "deleted_messages.db";
        private static final int DATABASE_VERSION = 2;

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
                    "reply_to_mid INTEGER DEFAULT 0, " +
                    "edit_date INTEGER DEFAULT 0, " +
                    "is_out INTEGER DEFAULT 0, " +
                    "PRIMARY KEY(dialog_id, message_id)" +
                    ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_del_dialog ON deleted_messages(dialog_id);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                try {
                    db.execSQL("ALTER TABLE deleted_messages ADD COLUMN reply_to_mid INTEGER DEFAULT 0;");
                    db.execSQL("ALTER TABLE deleted_messages ADD COLUMN edit_date INTEGER DEFAULT 0;");
                    db.execSQL("ALTER TABLE deleted_messages ADD COLUMN is_out INTEGER DEFAULT 0;");
                } catch (Exception ignore) {
                    db.execSQL("DROP TABLE IF EXISTS deleted_messages;");
                    onCreate(db);
                }
            } else {
                db.execSQL("DROP TABLE IF EXISTS deleted_messages;");
                onCreate(db);
            }
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

    public boolean saveDeletedMessageSync(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath, int replyToMid, int editDate, boolean isOut) {
        synchronized (fastDeletedLookup) {
            fastDeletedLookup.add(dialogId + "_" + messageId);
        }
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
            values.put("reply_to_mid", replyToMid);
            values.put("edit_date", editDate);
            values.put("is_out", isOut ? 1 : 0);
            long rowId = db.insertWithOnConflict("deleted_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            FileLog.d("[DeletedMessages] snapshot saved=" + (rowId != -1) + " dialogId=" + dialogId + " messageId=" + messageId);
            return rowId != -1;
        } catch (Exception e) {
            FileLog.e("[DeletedMessages] snapshot saved=false error=" + e.getMessage(), e);
            return false;
        }
    }

    public void saveDeletedMessageAsync(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath) {
        saveDeletedMessageAsync(dialogId, messageId, fromId, date, deleteDate, text, caption, mediaType, mediaPath, 0, 0, false);
    }

    public void saveDeletedMessageAsync(long dialogId, int messageId, long fromId, int date, int deleteDate, String text, String caption, String mediaType, String mediaPath, int replyToMid, int editDate, boolean isOut) {
        synchronized (fastDeletedLookup) {
            fastDeletedLookup.add(dialogId + "_" + messageId);
        }
        storageQueue.postRunnable(() -> {
            saveDeletedMessageSync(dialogId, messageId, fromId, date, deleteDate, text, caption, mediaType, mediaPath, replyToMid, editDate, isOut);
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

    public DeletedRecord getDeletedRecordSync(long dialogId, int messageId) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT dialog_id, message_id, from_id, date, delete_date, text, caption, media_type, media_path, reply_to_mid, edit_date, is_out FROM deleted_messages WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                DeletedRecord record = null;
                if (cursor.moveToFirst()) {
                    record = new DeletedRecord(
                            cursor.getLong(0),
                            cursor.getInt(1),
                            cursor.getLong(2),
                            cursor.getInt(3),
                            cursor.getInt(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getInt(9),
                            cursor.getInt(10),
                            cursor.getInt(11) == 1
                    );
                }
                cursor.close();
                return record;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public ArrayList<DeletedRecord> getDeletedMessagesForDialogSync(long dialogId) {
        ArrayList<DeletedRecord> list = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT dialog_id, message_id, from_id, date, delete_date, text, caption, media_type, media_path, reply_to_mid, edit_date, is_out FROM deleted_messages WHERE dialog_id = ? ORDER BY date DESC",
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
                            cursor.getString(8),
                            cursor.getInt(9),
                            cursor.getInt(10),
                            cursor.getInt(11) == 1
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
