package jp.techacademy.kawai.momoko.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static jp.techacademy.kawai.momoko.taskapp.R.string.no_category_selection;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.taro.kirameki.taskapp.TASK";
    public final static String EXTRA_CATEGORY = "jp.techacademy.taro.kirameki.taskapp.CATEGORY";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;
    List<String> mCategoryList;
    String mCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                String[] items=(String[])mCategoryList.toArray(new String[0]);
                intent.putExtra(EXTRA_CATEGORY, items);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);
                String[] items=(String[])mCategoryList.toArray(new String[0]);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());
                intent.putExtra(EXTRA_CATEGORY, items);

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する
                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });

        // (カテゴリ)の設定
        mCategoryList = new ArrayList<>();
        mCategory = getResources().getString(R.string.no_category_selection);
        Button button1 = (Button) findViewById(R.id.category_button);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("カテゴリ選択");
                getCategoryList();

                String[] items=(String[])mCategoryList.toArray(new String[0]);
                builder.setItems( items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                         mCategory = mCategoryList.get(which);
                        reloadListView();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // アプリ起動時に表示テスト用のタスクを作成する
        addTaskForTest();

        reloadListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }


    private void getCategoryList(){
        // 表示アイテムを指定する //
        mCategoryList.clear();
        mCategoryList.add(getResources().getString(R.string.no_category_selection));
        List<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("category", Sort.DESCENDING);
        String catName = "";
        for (int i = 0; i < taskRealmResults.size(); i++) {
            if (!(taskRealmResults.get(i).getCategory().equals(catName))) {
                catName = taskRealmResults.get(i).getCategory();
                mCategoryList.add(catName);
            }
        }
    }

    private void reloadListView() {
        getCategoryList();

        RealmResults<Task> taskRealmResults;
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        if (mCategory.equals(getResources().getString(R.string.no_category_selection))) {
            taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
        }
        else {
            RealmQuery<Task> taskRealmQuery = mRealm.where(Task.class).equalTo("category", mCategory);
            taskRealmResults = taskRealmQuery.findAllSorted("date", Sort.DESCENDING);
        }
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));

        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }

    private void addTaskForTest() {
        int taskID = 0;
        Task task = new Task();
        task.setTitle("作業１");
        task.setCategory("遊び");
        task.setContents("あれこれやる");
        task.setDate(new Date());
        task.setId(taskID);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(task);
        mRealm.commitTransaction();
        taskID++;

        task = new Task();
        task.setTitle("作業２");
        task.setCategory("仕事");
        task.setContents("いろいろする");
        task.setDate(new Date());
        task.setId(taskID);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(task);
        mRealm.commitTransaction();
        taskID++;

        task = new Task();
        task.setTitle("作業３");
        task.setCategory("");
        task.setContents("もごもごする");
        task.setDate(new Date());
        task.setId(taskID);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(task);
        mRealm.commitTransaction();
        taskID++;
    }
}
