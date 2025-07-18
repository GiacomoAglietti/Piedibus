package com.example.ids.main.stopList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ids.DBModels.Stop;
import com.example.ids.R;
import com.example.ids.main.MainActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

public class StopsRecyclerAdapter extends RecyclerView.Adapter<StopsRecyclerAdapter.ViewHolder> {
    private List<Stop> stopList;

    public StopsRecyclerAdapter(List<Stop> stopList) {
        this.stopList = stopList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.list_stops, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String numStopText = String.valueOf(position) + ".";
        holder.nameStopTextView.setText(stopList.get(position).getName_stop());
        holder.numStopTextView.setText(numStopText);

    }

    public void onItemMove(int fromPosition, int toPosition) {
        Stop fromP = stopList.get(fromPosition);
        Stop toP = stopList.get(toPosition);

        int supp = fromP.get_id();
        fromP.set_id(toP.get_id());
        toP.set_id(supp);

        Collections.sort(stopList);

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public int getItemCount() {
        if (stopList == null)
            return 0;
        else
            return stopList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView dragAndDropImgV;
        /**
         * ImageView to delete a stop, the user can also decide to cancel the deletion of the stop from the snackbar shown below
         */
        ImageView deleteImgV;
        TextView nameStopTextView;
        TextView numStopTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dragAndDropImgV = itemView.findViewById(R.id.drag_and_drop_imgV);
            deleteImgV = itemView.findViewById(R.id.delete_imgV);
            nameStopTextView = itemView.findViewById(R.id.name_stop_textV);
            numStopTextView = itemView.findViewById(R.id.num_stop_textV);

            deleteImgV.setOnClickListener(new View.OnClickListener() {
                private Stop deletedStop = null;

                @Override
                public void onClick(View view) {
                    final int pos = ViewHolder.this.getAbsoluteAdapterPosition();

                    deletedStop = stopList.get(pos);
                    stopList.remove(pos);
                    StopsRecyclerAdapter.this.notifyItemRemoved(pos);
                    StopsRecyclerAdapter.this.notifyItemRangeChanged(pos, MainActivity.totStop-1);
                    MainActivity.totStop--;
                    MainActivity.saveStopsImgView.setTag("enable");
                    MainActivity.saveStopsImgView.setImageResource(R.drawable.ic_save_stop_enable);

                    Snackbar.make(view, deletedStop.getName_stop(), Snackbar.LENGTH_LONG).setAction("Annulla", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            stopList.add(pos, deletedStop);
                            MainActivity.totStop++;
                            StopsRecyclerAdapter.this.notifyItemInserted(pos);
                            StopsRecyclerAdapter.this.notifyItemRangeChanged(pos,MainActivity.totStop-1);

                        }
                    }).show();
                }
            });

            itemView.setOnClickListener(this);
        }



        @Override
        public void onClick(View view) {

        }
    }
}
