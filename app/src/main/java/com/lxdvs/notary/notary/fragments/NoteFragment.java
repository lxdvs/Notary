package com.lxdvs.notary.notary.fragments;

/**
 * Created by alexdavis on 12/24/14.
 */
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Selection;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.lxdvs.notary.notary.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * A placeholder fragment containing a simple view.
 */
public class NoteFragment extends Fragment {

    EditText mWorkspace;
    InputMethodManager mKeyboard;

    public static final int PREF_LINES = 10;
    public static final String PREF_KEY = "ENDS";
    public static final String FILE = "notary_storage";

    public static boolean sTapStateHack = false;

    public NoteFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_notary, container, false);

        ViewTreeObserver vto = rootView.getViewTreeObserver();
        mWorkspace = (EditText)rootView.findViewById(R.id.workspace);
        mKeyboard = (InputMethodManager) container.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);


        String prefbuffer = getPrefString();

        // Hack for weird prefs bug appending four spaces to the buffer when you force-kill
        if (prefbuffer.endsWith("    ")) {
            prefbuffer = prefbuffer.substring(0, prefbuffer.length()-4);
        }

        mWorkspace.setText(prefbuffer);
        int position = mWorkspace.length();
        Editable etext = mWorkspace.getText();
        Selection.setSelection(etext, position);

        mFileTask.execute();

        if(vto.isAlive()){
            vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mWorkspace.requestFocus();
                    //mKeyboard.showSoftInput(mWorkspace, InputMethodManager.SHOW_FORCED);

                    final OnGlobalLayoutListener me = this;
                    rootView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ViewTreeObserver vto = rootView.getViewTreeObserver();
                            if(vto.isAlive()){
                                vto.removeOnGlobalLayoutListener(me);
                            }
                        }
                    }, 200);
                }
            });
        }

        mWorkspace.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View view, MotionEvent event) {
                //view.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction()&MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_MOVE:
                        mKeyboard.hideSoftInputFromWindow(mWorkspace.getWindowToken(), 0);
                        sTapStateHack = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        sTapStateHack = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (sTapStateHack) {
                            mKeyboard.showSoftInput(mWorkspace, InputMethodManager.SHOW_FORCED);
                        }

                }
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onStop() {
        super.onPause();
        String lines[] = mWorkspace.getText().toString().split("\\n");
        savePrefString(lines);
        saveDeferred(lines);
    }

    public void savePrefString(String[] lines) {
        String prefBuffer = "";
        int start = Math.max(0, lines.length - PREF_LINES);
        for (int i = start; i < lines.length; i++) {
            prefBuffer += lines[i]+"\n";
        }
        mWorkspace.getContext().getSharedPreferences("Notary", Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY, prefBuffer).apply();
    }

    public String getPrefString() {
        return mWorkspace.getContext().getSharedPreferences("Notary", Context.MODE_PRIVATE)
                .getString(PREF_KEY, "");
    }

    public void saveDeferred(String[] lines) {
        FileOutputStream outputStream;

        try {
            outputStream = mWorkspace.getContext().openFileOutput(FILE, Context.MODE_PRIVATE);
            int end = Math.max(0, lines.length - PREF_LINES);
            for (int i = 0; i < end; i++) {
                outputStream.write((lines[i]+"\n").getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AsyncTask<Void, Void, String> mFileTask = new AsyncTask<Void, Void, String>() {
        @Override
        protected String doInBackground(Void... params) {
            FileInputStream fis;
            String result = "";
            try {
                fis = mWorkspace.getContext().openFileInput(FILE);
                char current;
                while (fis.available() > 0) {
                    current = (char) fis.read();
                    result = result + String.valueOf(current);
                }
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        };

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            int start = mWorkspace.getSelectionStart();
            int end = mWorkspace.getSelectionEnd();
            mWorkspace.setText(s+mWorkspace.getText());
            mWorkspace.setSelection(s.length()+start, s.length()+end);
        }
    };
}