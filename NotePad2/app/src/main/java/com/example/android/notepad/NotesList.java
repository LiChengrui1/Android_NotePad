package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends Activity {

    // For logging and debugging
    private static final String TAG = "NotesList";
    private  static SimpleCursorAdapter adapter;
    private SearchView searchView;
    ListView list;

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE   //2
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        list = findViewById(R.id.mylist);
        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        SearchView();

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        list.setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = managedQuery(
            getIntent().getData(),            // Use the default content URI for the provider.
            PROJECTION,                       // Return the note ID , title and date,for each note.
            null,                             // No where clause, return all records.
            null,                             // No where clause, therefore no where column values.
            NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE , NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE  } ;



        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = { android.R.id.text1 ,android.R.id.text2};

        /* Creates the backing adapter for the ListView. */
        adapter = new SimpleCursorAdapter(
                      this,                             // The Context for the ListView
                      R.layout.noteslist_item,          // Points to the XML for a list item
                      cursor,                           // The cursor to get items from
                      dataColumns,
                viewIDs
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        list.setAdapter(adapter);

        // 为ListView的列表项的单击事件绑定事件监听器
        list.setOnItemClickListener((parent, view, position, id) -> {
            // Constructs a new URI from the incoming URI and the row ID
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

            // Gets the action from the incoming Intent
            String action = getIntent().getAction();

            // Handles requests for note data
            if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

                // Sets the result to return to the component that called this Activity. The
                // result contains the new URI
                setResult(RESULT_OK, new Intent().setData(uri));
            } else {

                // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
                // Intent's data is the note ID URI. The effect is to call NoteEdit.
                startActivity(new Intent(Intent.ACTION_EDIT, uri));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {   //长按删除

            @Override

            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(NotesList.this) //弹出一个对话框
                        .setMessage("确定要删除吗？")
                        .setNegativeButton("取消",new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton("确定",new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), id);
                                getContentResolver().delete(
                                        noteUri,  // The URI of the provider
                                        null,     // No where clause is needed, since only a single note ID is being
                                        // passed in.
                                        null      // No where clause is used, so no where arguments are needed.
                                );
                            }
                        })
                        .create()
                        .show();
                return true;
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
          /*
          新建
           */
           startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
           return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    private void SearchView(){
        searchView=findViewById(R.id.search);
        //设置没输入时的提示文字
        searchView.setQueryHint("搜索");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            //当提交时
            public boolean onQueryTextSubmit(String s) {
                return false;
            }
            // 当输入修改时
            @Override
            public boolean onQueryTextChange(String text) {
                Cursor cursor;
                String selection=null;
                if(!text.equals("")){
                    //查询条件
                    selection= NotePad.Notes.COLUMN_NAME_TITLE+" GLOB '*"+text+"*'";
                }
                cursor =  managedQuery(
                        getIntent().getData(),
                        PROJECTION,
                        selection,
                        null,
                        NotePad.Notes.DEFAULT_SORT_ORDER
                );
                adapter.swapCursor(cursor);
                return false;
            }
        });
    }


}
