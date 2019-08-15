package com.lmgy.redirect.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.lmgy.redirect.R;
import com.lmgy.redirect.adapter.HostSettingAdapter;
import com.lmgy.redirect.bean.HostData;
import com.lmgy.redirect.listener.RecyclerItemClickListener;
import com.lmgy.redirect.utils.SPUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HostSettingActivity extends AppCompatActivity implements ActionMode.Callback {

    private static final String TAG = "HostSettingActivity";
    private ActionMode actionMode;
    private boolean isMultiSelect = false;
    private List<String> selectedIds = new ArrayList<>();
    private HostSettingAdapter adapter;
    private CoordinatorLayout mCoordinatorLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_setting);
        initView();
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Hosts");
        initData();
    }

    private void initData(){
        adapter = new HostSettingAdapter(this, getList());
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mRv.setAdapter(adapter);

        mRv.addOnItemTouchListener(new RecyclerItemClickListener(this, mRv, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (isMultiSelect) {
                    multiSelect(position);
                } else {
                    Intent intent = new Intent(getApplicationContext(), ChangeSettingActivity.class);
                    Bundle mBundle = new Bundle();
                    mBundle.putSerializable("hostData", getList().get(position));
                    intent.putExtras(mBundle);
                    intent.putExtra("id", position);
                    startActivityForResult(intent, 0);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!isMultiSelect) {
                    selectedIds = new ArrayList<>();
                    isMultiSelect = true;
                    if (actionMode == null) {
                        actionMode = startActionMode(HostSettingActivity.this); //show ActionMode.
                    }
                }
                multiSelect(position);
            }
        }));

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter = new HostSettingAdapter(getApplicationContext(), getList());
                mRv.setAdapter(adapter);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 3) {
            Snackbar.make(mCoordinatorLayout, data.getStringExtra("result"), Snackbar.LENGTH_SHORT).show();
            adapter = new HostSettingAdapter(getApplicationContext(), getList());
            mRv.setAdapter(adapter);
        }
    }

    private void multiSelect(int position) {
        HostData data = adapter.getItem(position);
        if (data != null) {
            if (actionMode != null) {
                if (selectedIds.contains(String.valueOf(position)))
                    selectedIds.remove(String.valueOf(position));
                else
                    selectedIds.add(String.valueOf(position));

                if (selectedIds.size() > 0)
                    actionMode.setTitle(String.valueOf(selectedIds.size())); //show selected item count on action mode.
                else {
                    actionMode.setTitle("");
                    actionMode.finish();
                }
                adapter.setSelectedIds(selectedIds);
            }
        }
    }

    private List<HostData> getList() {
        return SPUtils.getDataList(this, "hostList", HostData.class);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_select, menu);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                final List<String> tempSelectedIds = selectedIds;
                new AlertDialog.Builder(this).setTitle(R.string.delete)//设置对话框标题
                        .setMessage(R.string.delete_message)//设置显示的内容
                        .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {//添加确定按钮
                            @Override
                            public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                Collections.sort(tempSelectedIds, Collections.<String>reverseOrder());
                                List<HostData> dataList = getList();
                                for (String id : tempSelectedIds) {
                                    dataList.remove(Integer.parseInt(id));
                                }
                                SPUtils.setDataList(getApplicationContext(), "hostList", dataList);
                                adapter = new HostSettingAdapter(getApplicationContext(), getList());
                                mRv.setAdapter(adapter);
                                Snackbar.make(mCoordinatorLayout, R.string.delete_successful, Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
                break;
            case R.id.action_toggle:
                List<HostData> hostDataList = getList();
                for (String id : selectedIds) {
                    HostData hostData = hostDataList.get(Integer.parseInt(id));
                    hostData.setType(!hostData.getType());
                }
                SPUtils.setDataList(this, "hostList", hostDataList);
                adapter = new HostSettingAdapter(getApplicationContext(), getList());
                mRv.setAdapter(adapter);
                break;
        }
        actionMode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        isMultiSelect = false;
        selectedIds = new ArrayList<>();
        adapter.setSelectedIds(new ArrayList<String>());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_add:
                Intent intent = new Intent(getApplicationContext(), ChangeSettingActivity.class);
                startActivityForResult(intent, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        mRv = (RecyclerView) findViewById(R.id.rv);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
    }
}
