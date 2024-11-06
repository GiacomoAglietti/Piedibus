package com.example.ids.main.Member;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ids.DBModels.Child;
import com.example.ids.DBModels.User;
import com.example.ids.R;
import com.example.ids.login.LoginActivity;
import com.example.ids.login.SocketHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.socket.client.Socket;

// source: https://github.com/android/views-widgets-samples/tree/main/RecyclerView
public class ParentsFragment extends Fragment {
    //socket for the node.js server
    private Socket socket;
    private Button getParentsbtn;
    private static final String TAG = "RecyclerViewFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2;
    private static final int DATASET_COUNT = 60;

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected ParentsAdapter adapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    // List<List<>> because there are multiple users whose children are retrieved from the database
    protected final List<List<Child>> children = new ArrayList<>();
    // List containing the group members
    protected final List<User> members = new ArrayList<>();
    protected HashMap<User, List<Child>> members_and_children = new HashMap<>();

    // Needed for synchronization between the fragment thread and db data retrieval by the node.js server
    private final Object lock = new Object();
    private Boolean flag_members_populated = false;
    private Boolean flag_children_populated = false;
    private int last_added_children_list = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SocketHandler socketHandler = SocketHandler.getInstance();
        socketHandler.setSocket();
        socketHandler.establishConnection();

        socket = SocketHandler.getInstance().getSocket();

        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        getGroupMembersProfiles(LoginActivity.groupID);
        java.util.Collections.sort(members);
        getRoleAndPhoneMembers();
        getChildrenOfGroupMembers();
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_parents, container, false);
        rootView.setTag(TAG);

        ExpandableListView expandableListView = rootView.findViewById(R.id.expandableListView);

        mLayoutManager = new LinearLayoutManager(getActivity());

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }

        // see function def
        //setExpandableListViewLayoutManager(mCurrentLayoutManagerType);

        adapter = new ParentsAdapter(getActivity(), members, members_and_children);
        expandableListView.setAdapter(adapter);

        return rootView;
    }

    // Keep for scroll position persistance
    public void setExpandableListViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        if (layoutManagerType == LayoutManagerType.GRID_LAYOUT_MANAGER) {
            mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
            mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
        } else {
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void getGroupMembersProfiles(String groupID) {
        if(groupID == null) {groupID = "";}

        SocketHandler socketHandler = SocketHandler.getInstance();
        socketHandler.setSocket();
        socketHandler.establishConnection();
        socket = SocketHandler.getInstance().getSocket();

        socket.on("getGroupMembersProfiles_sock", args -> {
            try{
                synchronized (members) {
                    JSONObject message = (JSONObject) args[0];
                    JSONArray users = message.getJSONArray("profiles");
                    try {
                        for (int i = 0; i < users.length(); i++) {
                            members.add(
                                    new User(
                                            users.getJSONObject(i).getString("user_id"),
                                            users.getJSONObject(i).getString("given_name")
                                    )
                            );
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(ParentsFragment.this.getActivity(), "Impossibile caricare la lista dei genitori", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    // procede to populate the "children" lists
                    flag_members_populated = true;
                    members.notifyAll();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        socket.emit("getGroupMembersProfiles_call", LoginActivity.userID, groupID);
    }
    private void getChildrenOfGroupMembers() {
        socket.on("getChildrenOfUser_sock", args -> {
            JSONObject message = (JSONObject) args[0];
            synchronized (children) {
                try {
                    JSONArray user_children = message.getJSONArray("children");
                    try {
                        flag_children_populated = false;
                        children.add(new ArrayList<>());

                        for (int i = 0; i < user_children.length(); i++) {
                            children.get(last_added_children_list).add(
                                    new Child(
                                            user_children.getJSONObject(i).getString("child_id"),
                                            user_children.getJSONObject(i).getString("given_name")
                                    )
                            );
                        }
                        last_added_children_list++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(ParentsFragment.this.getActivity(), "Impossibile caricare i bambini", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } catch(JSONException e){
                    e.printStackTrace();
                } finally {
                    // procede to put the i-th user's children list into the members_and_children
                    // hashmap
                    flag_children_populated = true;
                    children.notifyAll();
                }
            }
        });

        synchronized (members){
            while(!flag_members_populated){
                try {
                    members.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        members_and_children = new HashMap<>();
        for(User user : members){
            synchronized (children) {
                socket.emit("getChildrenOfUser_call", user.getUser_id());
                while(!flag_children_populated){
                    try {
                        children.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                members_and_children.put(user, children.get(last_added_children_list - 1));
                flag_children_populated = false;
            }
        }
    }

    private void getRoleAndPhoneMembers(){
        synchronized (members){
            while(!flag_members_populated){
                try {
                    members.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        for(User user : members) {
            user.update_from_db(socket);
        }
    }

}
