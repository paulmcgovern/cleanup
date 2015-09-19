package ca.pmcgovern.cleanup.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mcgovern on 8/22/15.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String TAG = "DBHelper";

    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "throwout.db";

    public DBHelper(Context context) {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    @Override
    public void onCreate(SQLiteDatabase db ) {
        Log.i(TAG, "Creating DB...");
// TODO: uniq constraint on start_date
        String sql = "CREATE TABLE CLEANUP_ROUND ( ID INTEGER PRIMARY KEY AUTOINCREMENT, START_DATE INTEGER NOT NULL, DURATION INTEGER NOT NULL, STATUS TEXT NOT NULL, REMINDERS INTEGER NOT NULL, NAME TEXT NOT NULL )";
        db.execSQL( sql );

                sql = "CREATE TABLE DISCARD_EVENT ( ROUND_ID INTEGER NOT NULL, DATE INTEGER NOT NULL, FOREIGN KEY(ROUND_ID) REFERENCES CLEANUP_ROUND(ID) )";
        db.execSQL( sql );

        Log.i(TAG, "...Created DB OK.");
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        Log.i( TAG, "Dropping tables..." );
        db.execSQL( "DROP TABLE IF EXISTS CLEANUP_ROUND" );
        db.execSQL( "DROP TABLE IF EXISTS DISCARD_EVENT" );
        Log.i(TAG, "...tables dropped OK.");
        onCreate(db);
    }

    public long insertRound( Round r ) {
        if( r == null ) {
            throw new IllegalArgumentException( "Round is null");
        }
        SQLiteDatabase db = this.getWritableDatabase();

        Date startDate = new Date( r.getStartDate() == 0 ? System.currentTimeMillis() : r.getStartDate());
        Calendar c = Calendar.getInstance();
        c.setTime( startDate );
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set( Calendar.MILLISECOND, 0 );



        ContentValues values = new ContentValues();
        values.put("START_DATE", c.getTimeInMillis());
        values.put("DURATION", r.getDurationDays() == 0 ? 1 : r.getDurationDays());
        values.put("STATUS", r.getStatus() == null ? Round.Status.IN_PROGRESS.toString() : r.getStatus().toString());
        values.put("REMINDERS", r.isSendReminders() ? 1 : 0);
        values.put("NAME", r.getName() == null ? "Round" : r.getName());

        long id = db.insert( "CLEANUP_ROUND", null, values );

  //      db.close();
        return id;
    }

    public void updateRound( Round r ) {
        if( r == null ) {
            throw new IllegalArgumentException( "Round is null" );
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put( "STATUS", r.getStatus().toString() );
        db.update("CLEANUP_ROUND", values, "ID=?", new String[]{Integer.toString(r.getRoundId())});
      //  db.close();
    }
    /**
     * The current round is the most recent round.
     * @return
     */
    public Round getCurrentRound() {

        String sql = "SELECT ID, START_DATE, DURATION, STATUS, REMINDERS, NAME FROM CLEANUP_ROUND ORDER BY START_DATE DESC LIMIT 1";

        Round r = new Round();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);


        if( c.getCount() >= 1 &&  c.moveToFirst() ) {
            r.setRoundId(c.getInt(0));
            r.setStartDate(c.getLong(1));
            r.setDurationDays(c.getInt(2));
            //r.setStatus( Round.Status.valueOf( c.getString( 3 )));

            if( isDone( r.getStartDate(), r.getDurationDays() )) {
                r.setStatus( Round.Status.DONE );
            } else {
                r.setStatus( Round.Status.valueOf( c.getString( 3 )));
            }

            r.setSendReminders( c.getInt( 4 ) == 1 );
            r.setName( c.getString( 5 ));


            if( !c.isClosed() ) {
                c.close();
            }
        } else {
            return null;
        }
        return r;
    }

    private boolean isDone(long startDate, int durationDays) {

        DateTime start = new DateTime( startDate );
        DateTime now   = new DateTime( System.currentTimeMillis() );

        int days = Days.daysBetween(start, now).getDays();

        Log.i(TAG, "Delta days: " + days + " duration:" + durationDays );

        return days > durationDays;

    }

    public void saveDiscardEvent( DiscardEvent de ) {

        if( de == null ) {
            throw new IllegalArgumentException( "DiscardEvent is null" );
        }

        int roundId = de.getRoundId();

        if( roundId <= 0 ) {
            throw new IllegalArgumentException( "Bad round ID " + roundId  );
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( "ROUND_ID", roundId );
        values.put( "DATE", de.getDiscardDate() == 0 ? System.currentTimeMillis() : de.getDiscardDate());

        db.insert( "DISCARD_EVENT", null, values );

    //    db.close();
    }

    public void clearAll() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM DISCARD_EVENT");
        db.execSQL("DELETE FROM CLEANUP_ROUND");
        db.setTransactionSuccessful();
        db.endTransaction();
  //      db.close();
    }


    public void undoDiscardEvent() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM DISCARD_EVENT WHERE DATE=(SELECT MAX(DATE) FROM DISCARD_EVENT)");
        db.setTransactionSuccessful();
        db.endTransaction();
  //      db.close();
    }

    public int getDiscardedTotal( int roundId ) {

        SQLiteDatabase db = this.getReadableDatabase();

        if( roundId < 1 ) {
            throw new IllegalArgumentException( "Bad round ID:" + roundId );
        }

        String sql = "SELECT COUNT(*) FROM DISCARD_EVENT WHERE ROUND_ID = " + roundId;

        int count = 0;
        Cursor c = db.rawQuery( sql, null );
        if( c.moveToFirst()) {
            count = c.getInt( 0 );
        }

        if( !c.isClosed()) {
            c.close();
        }
        return count;
    }


    public int getDiscardedToday() {

        SQLiteDatabase db = this.getReadableDatabase();

        Calendar now = Calendar.getInstance();
        now.set( Calendar.HOUR_OF_DAY, 0 );
        now.set( Calendar.MINUTE,      0 );
        now.set( Calendar.SECOND,      0 );
        now.set(Calendar.MILLISECOND, 0);
        long todayStart = now.getTimeInMillis();

        String sql = "SELECT COUNT(*) FROM DISCARD_EVENT WHERE DATE >= " + todayStart;

        int count = 0;
        Cursor c = db.rawQuery( sql, null );
        if( c.moveToFirst()) {
            count = c.getInt( 0 );
        }

        if( !c.isClosed()) {
            c.close();
        }
        return count;
    }


    public LinkedHashMap<Integer,Integer> getDiscardEventCountByDay( Round r ) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT cast(julianday(datetime( d.date / 1000, 'unixepoch', 'localtime' )) - julianday( datetime( r.start_date / 1000, 'unixepoch', 'localtime' )) AS INTEGER) AS round_day, COUNT(*) count FROM  discard_event d INNER JOIN cleanup_round  r  ON r.id=? AND d.round_id=r.id GROUP BY round_day";

        LinkedHashMap<Integer,Integer> countByDay = new LinkedHashMap<>();

        Cursor c = db.rawQuery( sql, new String[] { Integer.toString( r.getRoundId() )});
Log.i( TAG, sql + " " + Integer.toString( r.getRoundId()));
        if ( c.getCount() > 0 && c.moveToFirst() ) {
            do {
Log.i( TAG, " day:" + c.getInt(0) + " count:" + c.getInt( 1 ));
                countByDay.put( c.getInt(0), c.getInt(1));

            } while( c.moveToNext() );
        }

        if( !c.isClosed() ) {
            c.close();
        }
//        db.close();

        return countByDay;
    }

}
