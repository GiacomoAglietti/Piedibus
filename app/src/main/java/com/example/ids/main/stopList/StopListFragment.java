package com.example.ids.main.stopList;



import static com.example.ids.main.MainActivity.saveStopsImgView;



import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ids.R;
import com.example.ids.login.SocketHandler;
import com.example.ids.main.MainActivity;


public class StopListFragment extends Fragment {
    public static RecyclerView recyclerView;
    private StopsRecyclerAdapter stopsRecyclerAdapter;
    private DividerItemDecoration dividerItemDecoration = null;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stop_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.list_item);

        stopsRecyclerAdapter = new StopsRecyclerAdapter(MainActivity.stopList);
        recyclerView.setAdapter(stopsRecyclerAdapter);
        if (dividerItemDecoration == null){
            dividerItemDecoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(dividerItemDecoration);
        }


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(dragCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

    }

    ItemTouchHelper.Callback dragCallback = new ItemTouchHelper.Callback() {

        int dragFrom = -1;
        int dragTo = -1;

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP|ItemTouchHelper.DOWN|ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT,ItemTouchHelper.LEFT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAbsoluteAdapterPosition();
            int toPosition = target.getAbsoluteAdapterPosition();

            if(dragFrom == -1) {
                dragFrom =  fromPosition;
            }
            dragTo = toPosition;

            stopsRecyclerAdapter.onItemMove(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

        /**
         * To simulate the action of the "drop", in this way the numeration of the elements is recalculated only when the user drops the element dragged
          * @param from initial position of the dragged element
         * @param to final position of the dragged element
         */
        private void reallyMoved(int from, int to) {
            if(recyclerView.getAdapter() != null){
                if (from > to){
                    if(to == 0 )
                        from++;
                    recyclerView.getAdapter().notifyItemRangeChanged(to, from);
                }
                else{
                    if(from == 0 )
                        to++;
                    recyclerView.getAdapter().notifyItemRangeChanged(from, to);
                }
            }
            saveStopsImgView.setTag("enable");
            saveStopsImgView.setImageResource(R.drawable.ic_save_stop_enable);

        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                reallyMoved(dragFrom, dragTo);
            }

            dragFrom = dragTo = -1;
        }
    };

    /**
     *if saveStopsImgView is enabled it asks the user whether or not to keep the changes made
     */
    @Override
    public void onPause() {
        super.onPause();
        if(saveStopsImgView.getTag().equals("enable")){
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setMessage("Mantenere le modifiche effettuate nella lista delle fermete?");
            builder.setCancelable(false);
            builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveStopsImgView.callOnClick();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    saveStopsImgView.setTag("disable");
                    saveStopsImgView.setImageResource(R.drawable.ic_save_stop_disable);
                    try{
                        SocketHandler.getInstance().getSocket().emit("getAllStops");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.create().show();
        }
    }
}
