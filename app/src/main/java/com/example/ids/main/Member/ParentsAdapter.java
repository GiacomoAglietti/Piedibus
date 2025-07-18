package com.example.ids.main.Member;

import com.example.ids.DBModels.Child;
import com.example.ids.DBModels.User;

import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.ids.R;
import com.example.ids.login.LoginActivity;
import com.example.ids.login.SocketHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.socket.client.Socket;

/**
 * Adapter for the ParentsFragment fragment. Shows members as a list with some buttons for functionalities.
 */
public class ParentsAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {

    private Context context;
    private List<User> expandableListTitle;
    private HashMap<User, List<Child>> expandableListDetail;
    private String selectedRole;

    /**
     *
     * @param context
     * @param expandableListTitle group-level items (List of Users)
     * @param expandableListDetail child-level items (Hash map between a User and a list of their Children)
     */
    public ParentsAdapter(Context context, List<User> expandableListTitle,
                          HashMap<User, List<Child>> expandableListDetail){
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getGroupCount() {
        return expandableListTitle.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int count = 0;
        try{
            count = expandableListDetail.get(expandableListTitle.get(groupPosition)).size();
        }catch(NullPointerException npe){
            Log.d("Exception", "Nullpointer exception in CalendarioExpandableListAdapter:getChildrenCount()");
        }
        return count;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return expandableListTitle.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Object obj = null;
        try{
            obj = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(childPosition);
        }catch(NullPointerException npe){
            Log.d("Exception", "Nullpointer exception in CalendarioExpandableListAdapter:getChild()");
        }
        return obj;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        User user = (User) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_parents, null);
        }
        TextView listTitleTextView = convertView
                .findViewById(R.id.user_name_txtV);
        listTitleTextView.setText(user.getGiven_name());


        TextView role = convertView.findViewById(R.id.role_txtV);
        role.setText(user.getRole());
        ImageView changeRole = (ImageView) convertView.findViewById(R.id.change_role_imgV);

        if(!LoginActivity.permission.checkAdminPermission())
            changeRole.setVisibility(View.GONE);

        ImageView userPhone = (ImageView) convertView.findViewById(R.id.user_phone_imgV);
        changeRole.bringToFront();

        changeRole.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                showOptionDialog();
            }


            private void showOptionDialog() {
                String[] roles = {"genitore", "guida", "admin"};

                int pos = Arrays.asList(roles).indexOf(user.getRole());

                // "Change role" alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Scegli il ruolo da assegnare");
                builder.setSingleChoiceItems(roles, pos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedRole = roles[i];
                    }
                });
                builder.setPositiveButton("Salva", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Socket socket = SocketHandler.getInstance().getSocket();

                        socket.emit("updateRoleOfUser_call", user.getUser_id(), LoginActivity.groupID, selectedRole);
                        user.setRole(selectedRole);
                        role.setText(user.getRole());
                        Log.d("User builder", user.toString());
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                // end
            }

        });

        // shows an alert with the user phone
        userPhone.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Contatto telefonico");
            builder.setMessage(user.getPhone());
            builder.setCancelable(true);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        });
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final Child child = (Child) getChild(groupPosition, childPosition);
        if(convertView == null){
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_children, null);
        }

        TextView expandedListTextView = convertView.findViewById(R.id.expanded_list_item);
        expandedListTextView.setText(child.getGiven_name());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return this.expandableListTitle.isEmpty();
    }

    @Override
    public void onGroupExpanded(int groupPosition) {

    }

    @Override
    public void onGroupCollapsed(int groupPosition) {

    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return 0;
    }


}
