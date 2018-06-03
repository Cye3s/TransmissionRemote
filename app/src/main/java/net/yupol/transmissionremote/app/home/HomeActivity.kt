package net.yupol.transmissionremote.app.home

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.home_activity.*
import net.yupol.transmissionremote.app.BaseActivity
import net.yupol.transmissionremote.app.R
import net.yupol.transmissionremote.app.TransmissionRemote
import net.yupol.transmissionremote.app.preferences.PreferencesActivity
import net.yupol.transmissionremote.app.preferences.ServerPreferencesActivity
import net.yupol.transmissionremote.app.preferences.ServersActivity
import net.yupol.transmissionremote.app.server.AddServerActivity
import net.yupol.transmissionremote.app.sorting.SortOrder
import net.yupol.transmissionremote.app.sorting.SortedBy
import net.yupol.transmissionremote.app.utils.ThemeUtils
import net.yupol.transmissionremote.model.Server

class HomeActivity: BaseActivity(), Drawer.Listener {

    companion object {
        const val REQUEST_CODE_SERVER_PARAMS = 1

        const val FRAGMENT_TAG_TORRENT_LIST = "fragment_tag_torrent_list"
    }

    private lateinit var app: TransmissionRemote

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        app = TransmissionRemote.getInstance()

        val drawer = Drawer(this)
        val servers = app.servers
        val activeServer = app.activeServer
        val sortedBy = app.sortedBy
        val sortOrder = app.sortOrder
        drawer.setupDrawer(this, toolbar, servers, activeServer, sortedBy, sortOrder)

        if (app.activeServer != null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TorrentListFragment2.newInstance(), FRAGMENT_TAG_TORRENT_LIST)
                    .commit()
        }
    }

    override fun onServerSettingsPressed() {
        startActivity(Intent(this, ServerPreferencesActivity::class.java))
    }

    override fun onAddServerPressed() {
        val intent = Intent(this, AddServerActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SERVER_PARAMS)
    }

    override fun onManageServersPressed() {
        startActivity(Intent(this, ServersActivity::class.java))
    }

    override fun onServerSelected(server: Server) {
        TODO("not implemented")
    }

    override fun onSettingsPressed() {
        startActivity(Intent(this, PreferencesActivity::class.java))
    }

    override fun onSortingChanged(sortedBy: SortedBy, sortOrder: SortOrder) {
        app.setSorting(sortedBy, sortOrder)
    }

    override fun onThemeSwitched(nightMode: Boolean) {
        ThemeUtils.setIsInNightMode(this, nightMode)
        recreate()
    }
}