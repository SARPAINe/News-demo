package storage.sql.newsreader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> linkId=new ArrayList<>();
    ArrayList<String> titles=new ArrayList<>();
    ArrayList<String> content=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ListView listView;
    SQLiteDatabase articlesDB;


    DownloadTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articlesDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,title varchar,content varchar) ");


        task=new DownloadTask();
        try{

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json");

        }
        catch (Exception e){
            Toast.makeText(this, "There seems to be an error! Please check your internet connection.", Toast.LENGTH_SHORT).show();
        }
        listView=findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),SecondActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);

            }
        });
        updateListView();


    }

    public void updateListView(){
        Cursor c=articlesDB.rawQuery("SELECT * FROM articles",null);
        int titleIndex=c.getColumnIndex("title");
        int contentIndex=c.getColumnIndex("content");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }
    public class DownloadTask extends AsyncTask<String, Void ,String>{

        @Override
        protected String doInBackground(String... urls) {
            String result="";
            URL url;
            HttpURLConnection httpURLConnection;
            try {
                url=new URL(urls[0]);
                httpURLConnection= (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
                InputStream in=httpURLConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                int data=reader.read();
                while (data!=-1){
                    char current= (char) data;
                    result+=current;
                    data=reader.read();
                }

                JSONArray array=new JSONArray(result);
                int numberOfItems=10;
                if(array.length()<20)
                    numberOfItems=array.length();
                articlesDB.execSQL("DELETE FROM articles");
                for(int i=0;i<numberOfItems;i++)
                {
//                   Log.i("id "+i,array.get(i).toString());

                    String articleId=array.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    httpURLConnection= (HttpURLConnection) url.openConnection();
                    httpURLConnection.connect();
                    in=httpURLConnection.getInputStream();
                    reader=new InputStreamReader(in);
                    data=reader.read();
                    String articleInfo="";
                    while (data!=-1){
                        char current= (char) data;
                        articleInfo+=current;
                        data=reader.read();
                    }

                    JSONObject jsonObject=new JSONObject(articleInfo);
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle=jsonObject.getString("title");
                        String articleUrl=jsonObject.getString("url");
                        Log.i("title",articleTitle);
                        Log.i("url",articleUrl);
                        url=new URL(articleUrl);
                        httpURLConnection= (HttpURLConnection) url.openConnection();
                        httpURLConnection.connect();
                        in=httpURLConnection.getInputStream();
                        reader=new InputStreamReader(in);
                        data=reader.read();
                        String articleContent="";
                        while (data!=-1){
                            char current= (char) data;
                            articleContent+=current;
                            data=reader.read();
                        }
                        String sql="INSERT INTO articles (articleId,title,content) VALUES(? , ? ,?)";
                        SQLiteStatement sqLiteStatement=articlesDB.compileStatement(sql);
                        sqLiteStatement.bindString(1,articleId);
                        sqLiteStatement.bindString(2,articleTitle);
                        sqLiteStatement.bindString(1,articleContent);
                        sqLiteStatement.execute();
                    }
                }
                Log.i("url content",result);
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                return "failed!";
            }

        }


    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        updateListView();
    }
}
