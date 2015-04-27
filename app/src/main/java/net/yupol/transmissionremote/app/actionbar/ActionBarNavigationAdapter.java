package net.yupol.transmissionremote.app.actionbar;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.common.collect.FluentIterable;

import net.yupol.transmissionremote.app.R;
import net.yupol.transmissionremote.app.TransmissionRemote;
import net.yupol.transmissionremote.app.filtering.Filter;
import net.yupol.transmissionremote.app.server.Server;

public class ActionBarNavigationAdapter extends BaseAdapter {

    private static final String TAG = ActionBarNavigationAdapter.class.getSimpleName();

    public static final int ID_SERVER = 0;
    public static final int ID_FILTER = 1;
    private static final int ID_SERVER_TITLE = 2;
    private static final int ID_FILTER_TITLE = 3;

    private Context context;
    private TransmissionRemote app;

    public ActionBarNavigationAdapter(Context context) {
        this.context = context;
        app = (TransmissionRemote) context.getApplicationContext();
    }

    @Override
    public int getCount() {
        // server title + servers + filter title + filters
        return 1 + app.getServers().size() + 1 + app.getAllFilters().length;
    }

    @Override
    public Object getItem(int position) {
        long id = getItemId(position);
        switch ((int) id) {
            case ID_SERVER_TITLE:
            case ID_FILTER_TITLE:
                return null;
            case ID_SERVER:
                return app.getServers().get(position - 1);
            case ID_FILTER:
                return app.getAllFilters()[position - app.getServers().size() - 2];
        }
        Log.e(TAG, "Unknown item at position " + position +
                ". Number of servers: " + app.getServers().size() +
                ", number of filters: " + app.getAllFilters().length);
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) return ID_SERVER_TITLE;
        if (position <= app.getServers().size()) return ID_SERVER;
        if (position == app.getServers().size() + 1) return ID_FILTER_TITLE;
        return ID_FILTER;
    }

    @Override
    public boolean isEnabled(int position) {
        long id = getItemId(position);
        return id == ID_SERVER || id == ID_FILTER;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        long id = getItemId(position);
        View itemView = convertView;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = li.inflate(R.layout.drop_down_navigation_item, parent, false);
        }

        TextView text = (TextView) itemView.findViewById(R.id.text);
        TextView countText = (TextView) itemView.findViewById(R.id.torrent_count);
        TextView headerText = (TextView) itemView.findViewById(R.id.header_text);
        View separator = itemView.findViewById(R.id.separator);

        if (id == ID_SERVER_TITLE || id == ID_FILTER_TITLE) {
            text.setVisibility(View.GONE);
            countText.setVisibility(View.GONE);
            headerText.setVisibility(View.VISIBLE);
            separator.setVisibility(View.VISIBLE);

            headerText.setText(id == ID_SERVER_TITLE ? R.string.servers : R.string.filters);
        } else {
            text.setVisibility(View.VISIBLE);
            headerText.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);

            if (id == ID_SERVER) {
                countText.setVisibility(View.GONE);
                Server server = (Server) getItem(position);
                text.setText(server.getName());
                text.setTextColor(textColor(server.equals(app.getActiveServer())));
            } else if (id == ID_FILTER) {
                countText.setVisibility(View.VISIBLE);
                Filter filter = (Filter) getItem(position);
                text.setText(filter.getNameResId());
                countText.setText(String.valueOf(FluentIterable.from(app.getTorrents()).filter(filter).size()));

                int textColor = textColor(filter.equals(app.getActiveFilter()));
                text.setTextColor(textColor);
                countText.setTextColor(textColor);
            }
        }

        return itemView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.drop_down_navigation, parent, false);
        }

        TextView serverName = (TextView) view.findViewById(R.id.server_name);
        serverName.setText(app.getActiveServer().getName());

        TextView filterName = (TextView) view.findViewById(R.id.filter_name);
        filterName.setText(app.getActiveFilter().getNameResId());

        return view;
    }

    private int textColor(boolean isActive) {
        return context.getResources().getColor(isActive
                ? R.color.drop_down_navigation_active_item_text_color
                : R.color.drop_down_navigation_item_text_color);
    }
}
