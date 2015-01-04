package net.yupol.transmissionremote.app;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import net.yupol.transmissionremote.app.drawer.Drawer;
import net.yupol.transmissionremote.app.drawer.DrawerGroupItem;
import net.yupol.transmissionremote.app.drawer.DrawerItem;
import net.yupol.transmissionremote.app.drawer.NewServerDrawerItem;
import net.yupol.transmissionremote.app.drawer.ServerDrawerItem;
import net.yupol.transmissionremote.app.drawer.ServerPrefsDrawerItem;
import net.yupol.transmissionremote.app.drawer.SortDrawerGroupItem;
import net.yupol.transmissionremote.app.model.json.PortTestResult;
import net.yupol.transmissionremote.app.preferences.ServerPreferencesActivity;
import net.yupol.transmissionremote.app.server.AddServerActivity;
import net.yupol.transmissionremote.app.server.Server;
import net.yupol.transmissionremote.app.transport.BaseSpiceActivity;
import net.yupol.transmissionremote.app.transport.Torrent;
import net.yupol.transmissionremote.app.transport.request.PortTestRequest;
import net.yupol.transmissionremote.app.transport.request.Request;
import net.yupol.transmissionremote.app.transport.request.SessionSetRequest;
import net.yupol.transmissionremote.app.transport.response.CheckPortResponse;
import net.yupol.transmissionremote.app.transport.response.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends BaseSpiceActivity implements Drawer.OnItemSelectedListener,
            TorrentUpdater.TorrentUpdateListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static int REQUEST_CODE_SERVER_PARAMS = 1;
    public static int REQUEST_CODE_SERVER_PREFERENCES = 2;

    private TransmissionRemote application;
    private Queue<Request> pendingRequests = new LinkedList<>();
    private TorrentUpdater torrentUpdater;

    private Drawer drawer;
    private ActionBarDrawerToggle drawerToggle;
    private TorrentListFragment torrentListFragment;
    private ToolbarFragment toolbarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        application = TransmissionRemote.getApplication(this);

        ListView drawerList = (ListView) findViewById(R.id.drawer_list);

        drawer = new Drawer(drawerList);
        for (Server server : application.getServers())
            drawer.addServer(server);
        drawer.setOnItemSelectedListener(this);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                    R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getActionBar() != null) getActionBar().setTitle(getTitle());
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (getActionBar() != null) getActionBar().setTitle(getTitle());
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        FragmentManager fm = getFragmentManager();

        torrentListFragment = (TorrentListFragment) fm.findFragmentById(R.id.torrent_list_container);
        if (torrentListFragment == null) {
            torrentListFragment = new TorrentListFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.torrent_list_container, torrentListFragment);
            ft.commit();
        }

        toolbarFragment = (ToolbarFragment) fm.findFragmentById(R.id.toolbar_container);
        if (toolbarFragment == null) {
            toolbarFragment = new ToolbarFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.toolbar_container, toolbarFragment);
            ft.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        List<Server> servers = application.getServers();
        if (servers.isEmpty()) {
            Intent intent = new Intent(this, AddServerActivity.class);
            intent.putExtra(AddServerActivity.PARAM_CANCELABLE, false);
            startActivityForResult(intent, REQUEST_CODE_SERVER_PARAMS);
        } else {
            torrentUpdater = new TorrentUpdater(getTransportManager(), this, application.getUpdateInterval());

            getTransportManager().doRequest(new PortTestRequest(), new RequestListener<PortTestResult>() {
                @Override
                public void onRequestFailure(SpiceException spiceException) {

                }

                @Override
                public void onRequestSuccess(PortTestResult result) {
                    if (result.isOpen()) {
                        torrentUpdater.start();
                    } else {
                        Toast.makeText(MainActivity.this, "Port " + application.getActiveServer().getPort() +
                                " is closed. Check Transmission settings.", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Port " + application.getActiveServer().getPort() + " is closed");
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        if (torrentUpdater != null)
            torrentUpdater.stop();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode == REQUEST_CODE_SERVER_PARAMS) {
            if (resultCode == RESULT_OK) {
                Server server = data.getParcelableExtra(AddServerActivity.EXTRA_SEVER);
                addNewServer(server);
            }
        } else if (requestCode == REQUEST_CODE_SERVER_PREFERENCES) {
            if (resultCode == RESULT_OK) {
                String prefsExtra = data.getStringExtra(ServerPreferencesActivity.EXTRA_SERVER_PREFERENCES);
                JSONObject preferences;
                try {
                    preferences = new JSONObject(prefsExtra);
                } catch (JSONException e) {
                    Log.e(TAG, "Can't parse session preferences as JSON object: '" + prefsExtra + "'");
                    return;
                }

                Log.d(TAG, "preferences: " + preferences);

                pendingRequests.offer(new SessionSetRequest(preferences));
            }
        }
    }

    @Override
    public void onDrawerItemSelected(DrawerGroupItem group, DrawerItem item) {
        Log.d(TAG, "item '" + item.getText() + "' in group '" + group.getText() + "' selected");

        item.itemSelected();

        if (group.getId() == Drawer.Groups.SERVERS.id()) {
            if (item instanceof NewServerDrawerItem) {
                startActivityForResult(new Intent(this, AddServerActivity.class), REQUEST_CODE_SERVER_PARAMS);
            } else if (item instanceof ServerDrawerItem) {
                Server server = ((ServerDrawerItem) item).getServer();
                if (server != application.getActiveServer()) {
                    application.setActiveServer(server);
                }
            }
        } else if (group.getId() == Drawer.Groups.SORT_BY.id()) {
            ((SortDrawerGroupItem) group).itemSelected(item);
            drawer.refresh();

            if (torrentListFragment != null)
                torrentListFragment.setSort(((SortDrawerGroupItem) group).getComparator());
        } else if (group.getId() == Drawer.Groups.PREFERENCES.id()) {
            if (item instanceof ServerPrefsDrawerItem) {
                startActivityForResult(
                        new Intent(this, ServerPreferencesActivity.class),
                        REQUEST_CODE_SERVER_PREFERENCES);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTorrentUpdate(List<Torrent> torrents) {
        if (torrentListFragment != null) {
            torrentListFragment.torrentsUpdated(torrents);
            toolbarFragment.torrentsUpdated(torrents);
        }

        String text = Joiner.on("\n").join(FluentIterable.from(torrents).transform(new Function<Torrent, String>() {
            @Override
            public String apply(Torrent torrent) {
                String percents = String.format("%.2f", torrent.getPercentDone() * 100);
                return torrent.getStatus() + " " + percents + "% " + torrent.getName();
            }
        }));

        Log.d(TAG, "Torrents:\n" + text);
    }

    private void addNewServer(Server server) {
        application.addServer(server);
        drawer.addServer(server);

        if (application.getServers().size() == 1) {
            application.setActiveServer(server);
            torrentUpdater = new TorrentUpdater(getTransportManager(), this, application.getUpdateInterval());
            // TODO: drawer.setActiveServer(server);
        }
    }

    private void handleResponse(Message msg) {
        if (!(msg.obj instanceof Response))
            throw new IllegalArgumentException("Response message must contain Response object in its 'obj' field");

        Response response = (Response) msg.obj;

        Log.d(TAG, "Response received: " + response.getBody());

        if (response instanceof CheckPortResponse) {
            boolean isOpen = ((CheckPortResponse) response).isOpen();
            if (isOpen) {
                //sendRequest(new UpdateTorrentsRequest());
                torrentUpdater.start();
            } else {
                Toast.makeText(this, "Port " + application.getActiveServer().getPort() +
                        " is closed. Check Transmission settings.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Port " + application.getActiveServer().getPort() + " is closed");
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.update_interval_key))) {
            if (torrentUpdater != null) {
                torrentUpdater.setTimeout(application.getUpdateInterval());
            }
        }
    }
}
