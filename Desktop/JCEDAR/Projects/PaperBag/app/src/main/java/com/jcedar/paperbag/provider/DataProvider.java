package com.jcedar.paperbag.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.jcedar.paperbag.helper.SelectionBuilder;


/**
 * Created by OLUWAPHEMMY on 10/30/2016.
 */
public class DataProvider extends ContentProvider {

    private static final String TAG = DataProvider.class.getName();
    private UriMatcher sUriMatcher = buildUriMatcher();
    private  DatabaseHelper mHelper;
    private SQLiteDatabase mdb;

    private static final int MY_PRODUCT_ID = 101;
    private static final int MY_PRODUCT_LIST = 102;

    private static final int MY_FAV_ID = 201;
    private static final int MY_FAV_LIST = 202;


    @Override
    public boolean onCreate() {

        mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        mdb = mHelper.getReadableDatabase();
        int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = buildSelection(uri, match);
        switch (match) {
            case MY_PRODUCT_LIST:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = PaperBagContract.MyProduct.SORT_ORDER_DEFAULT;
                }
                break;
            case MY_FAV_LIST:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = PaperBagContract.MyFavorite.SORT_ORDER_DEFAULT;
                }
                break;
            default:
                break;
        }
        return builder.where(selection, selectionArgs).query(mdb, projection, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        switch (sUriMatcher.match(uri)){
            case MY_PRODUCT_ID:
                return PaperBagContract.MyProduct.CONTENT_ITEM_TYPE;
            case MY_PRODUCT_LIST:
                return PaperBagContract.MyProduct.CONTENT_TYPE;
            case MY_FAV_ID:
                return PaperBagContract.MyFavorite.CONTENT_ITEM_TYPE;
            case MY_FAV_LIST:
                return PaperBagContract.MyFavorite.CONTENT_TYPE;


            default:
                throw new IllegalArgumentException ("Unsupported Uri: " +uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        mdb = mHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = buildSelection(uri, match);
        long id;

        switch (match){
            case MY_PRODUCT_LIST: {
                id = mdb.insertOrThrow(DatabaseHelper.Tables.MY_PRODUCT, null, contentValues);
                notifyChange(uri);
                return getUriForId(id, uri);
            }
            case MY_FAV_LIST: {
                id = mdb.insertOrThrow(DatabaseHelper.Tables.MY_FAV, null, contentValues);
                notifyChange(uri);
                return getUriForId(id, uri);
            }

            default:
                throw new UnsupportedOperationException ("Unsupported insert Uri: " + uri);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        mdb = mHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int numRowsInsert = 0;
        switch (match) {
            case MY_PRODUCT_LIST:
                mdb.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = mdb.insert(DatabaseHelper.Tables.MY_PRODUCT, null, value);
                        if (_id != -1) {
                            numRowsInsert++;
                        }
                    }
                    // To commit the transaction
                    mdb.setTransactionSuccessful();
                } finally {
                    mdb.endTransaction();
                }
                break;
            case MY_FAV_LIST:
                mdb.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = mdb.insert(DatabaseHelper.Tables.MY_FAV, null, value);
                        if (_id != -1) {
                            numRowsInsert++;
                        }
                    }
                    // To commit the transaction
                    mdb.setTransactionSuccessful();
                } finally {
                    mdb.endTransaction();
                }
                break;
            default:
                return super.bulkInsert(uri, values);
        }

        if (numRowsInsert > 0) {
            notifyChange(uri);
        }
        return numRowsInsert;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if (uri == PaperBagContract.BASE_CONTENT_URI){
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        mdb = mHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = buildSelection(uri, match);
        int retValue = builder.where(selection, selectionArgs).delete(mdb);
        notifyChange(uri);
        return retValue ;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        mdb = mHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = buildSelection(uri, match);

        int retVal = builder.where(selection, selectionArgs).update(mdb, contentValues);
        notifyChange(uri);
        return retVal;
    }

    private static Uri getUriForId(long id, Uri uri) {
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            return itemUri;
        }
        //something went wrong
        throw new SQLException("Problem inserting into uri: " + uri);
    }

    private void notifyChange(Uri uri) {
        Context context = getContext();
        if( context != null) {
            ContentResolver resolver = context.getContentResolver();
            resolver.notifyChange(uri, null);
        }

        // Widgets can't register content observers so we refresh widgets separately.
        // context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PaperBagContract.CONTENT_AUTHORITY;


        matcher.addURI(authority, PaperBagContract.PATH_CART, MY_PRODUCT_LIST);
        matcher.addURI(authority, PaperBagContract.PATH_CART + "/#", MY_PRODUCT_ID);

        matcher.addURI(authority, PaperBagContract.PATH_FAV, MY_FAV_LIST);
        matcher.addURI(authority, PaperBagContract.PATH_FAV + "/#", MY_FAV_ID);

        return matcher;
    }

    private SelectionBuilder buildSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case MY_PRODUCT_LIST: {
                return builder.table(DatabaseHelper.Tables.MY_PRODUCT);
            }
            case MY_PRODUCT_ID: {
                final String id = uri.getLastPathSegment();
                return builder.table(DatabaseHelper.Tables.MY_PRODUCT)
                        .where(PaperBagContract.MyProduct._ID + "=?", id);
            }
            case MY_FAV_LIST: {
                return builder.table(DatabaseHelper.Tables.MY_FAV);
            }
            case MY_FAV_ID: {
                final String id = uri.getLastPathSegment();
                return builder.table(DatabaseHelper.Tables.MY_FAV)
                        .where(PaperBagContract.MyFavorite._ID + "=?", id);
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mHelper.close();
        Context context = getContext();
        DatabaseHelper.deleteDatabase(context);
        mHelper = new DatabaseHelper(getContext());
    }
}
