package jp.techacademy.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import io.realm.OrderedRealmCollection
import io.realm.RealmResults
import java.util.*
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.inputmethod.InputMethodManager


const val EXTRA_TASK = "jp.techacademy.taskapp.TASK"


class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm>{
        override fun onChange(element: Realm){
            reloadListView()
        }
    }
    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)


        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //検索ボタンをタップされたときの処理 課題で追加
        search_button.setOnClickListener{view ->
            //キーボードを隠す
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            //テキストエディットからテキスト取得
            var searchText = search_edit_text.text.toString()
            Log.d("testtest", searchText)

            if(searchText != ""){
                Log.d("testtest", "何かしら入っている")
                val searchResults = mRealm.where(Task::class.java).equalTo("category", searchText).findAll()
                val s = searchResults.size
                Log.d("testtest", searchResults.toString())

                if(s != 0){
                    Log.d("testtest", "検索結果がnullじゃない")

                    // 上記の結果を、Tasklistとしてセットする
                    mTaskAdapter.taskList = mRealm.copyFromRealm(searchResults)

                    //TaskのListView用のアダプタにわたす
                    listView1.adapter = mTaskAdapter

                    // 表示を更新するために、アダプターにデータが変更されることを知らせる
                    mTaskAdapter.notifyDataSetChanged()

                    }else{

                        Log.d("testtest", "検索結果なし")
                        val builder = AlertDialog.Builder(this@MainActivity)

                        builder.setTitle("検索結果")
                        builder.setMessage(searchText + "は見つかりませんでした")

                        builder.setPositiveButton("リセットする") { _, _ ->
                            reloadListView()
                        }

                    val dialog = builder.create()
                    dialog.show()

                    }

                } else{
                Log.d("testtest", "キーワードスペースも何も入っていない")
                    val builder = AlertDialog.Builder(this@MainActivity)

                    builder.setTitle("検索結果")
                    builder.setMessage(searchText + "キーワードを入れてください")

                    builder.setPositiveButton("リセットする") { _, _ ->
                        reloadListView()
                    }

                val dialog = builder.create()
                dialog.show()

                }
            search_edit_text.text = null
        }

        //ListViewをタップしたときの処理
        listView1.setOnItemClickListener{parent, view, position, id ->
            //　入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            //タスクを削除する
            val task = parent.adapter.getItem(position) as Task
            //ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultsIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }

    private fun reloadListView(){
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、Tasklistとしてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        //TaskのListView用のアダプタにわたす
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されることを知らせる
        mTaskAdapter.notifyDataSetChanged()

    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

}
