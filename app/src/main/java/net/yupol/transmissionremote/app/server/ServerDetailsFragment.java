package net.yupol.transmissionremote.app.server;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import net.yupol.transmissionremote.app.R;

import org.apache.commons.lang3.StringUtils;

public class ServerDetailsFragment extends Fragment {

    public static final String ARGUMENT_SERVER = "argument_server";

    private static final String TAG = ServerDetailsFragment.class.getSimpleName();
    private static final int DEFAULT_PORT = 9091;
    private static final String KEY_IS_AUTH_ENABLED = "key_is_auth_enabled";
    private static final String CHARS_TO_STRIP_RPC = "/";

    private boolean isAuthEnabled = false;

    private EditText serverNameEdit;
    private EditText hostNameEdit;
    private EditText portNumberEdit;
    private CheckBox authCheckBox;
    private EditText userNameEdit;
    private EditText passwordEdit;
    private EditText rpcUrlEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.server_details_fragment, container, false);

        serverNameEdit = (EditText) view.findViewById(R.id.server_name_edit_text);
        hostNameEdit = (EditText) view.findViewById(R.id.host_edit_text);
        portNumberEdit = (EditText) view.findViewById(R.id.port_edit_text);
        authCheckBox = (CheckBox) view.findViewById(R.id.aunthentication_checkbox);
        userNameEdit = (EditText) view.findViewById(R.id.user_name_edit_text);
        passwordEdit = (EditText) view.findViewById(R.id.password_edit_text);
        rpcUrlEdit = (EditText) view.findViewById(R.id.rpc_url_edit_text);
        Button defaultRpcUrlBtn = (Button) view.findViewById(R.id.default_rpc_url_button);
        defaultRpcUrlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rpcUrlEdit.setText(Server.DEFAULT_RPC_URL);
            }
        });

        portNumberEdit.setFilters(new InputFilter[]{PortNumberFilter.instance()});

        authCheckBox.setChecked(isAuthEnabled);
        updateAuth(isAuthEnabled);

        authCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateAuth(authCheckBox.isChecked());
            }
        });

        Server server = getServerArgument();
        rpcUrlEdit.setText(server != null ? server.getRpcUrl() : Server.DEFAULT_RPC_URL);

        serverNameEdit.addTextChangedListener(new ClearErrorTextWatcher(serverNameEdit));
        hostNameEdit.addTextChangedListener(new ClearErrorTextWatcher(hostNameEdit));
        portNumberEdit.addTextChangedListener(new ClearErrorTextWatcher(portNumberEdit));

        updateUI(server);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_AUTH_ENABLED, isAuthEnabled);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_IS_AUTH_ENABLED)) {
            isAuthEnabled = savedInstanceState.getBoolean(KEY_IS_AUTH_ENABLED);
            updateAuth(isAuthEnabled);
        }
    }

    public Server getNewServer() {
        if (!checkValidity())
            return null;

        Server server;
        if (isAuthEnabled) {
            server = new Server(getUiName(), getUiHost(), getUiPort(), getUiUserName(), getUiPassword());
        } else {
            server = new Server(getUiName(), getUiHost(), getUiPort());
        }

        return server;
    }

    public Server getServerArgument() {
        Server server = null;
        if (getArguments() != null)
            server = getArguments().getParcelable(ARGUMENT_SERVER);
        return server;
    }

    public void saveServer() {
        Server server = getServerArgument();
        if (server == null) {
            throw new IllegalStateException("No server argument, can't save server");
        }
        server.setName(getUiName());
        server.setHost(getUiHost());
        server.setPort(getUiPort());
        server.setAuthenticationEnabled(isAuthEnabled);
        server.setUserName(getUiUserName());
        server.setPassword(getUiPassword());
        server.setRpcUrl(getUiRpcUrl());
    }

    public boolean hasChanges() {
        Server server = getServerArgument();
        if (server == null) {
            return true;
        }
        if (!getUiName().equals(server.getName()))
            return true;
        if (!getUiHost().equals(server.getHost()))
            return true;
        if (getUiPort() != server.getPort())
            return true;
        if (isAuthEnabled != server.isAuthenticationEnabled())
            return true;
        if (!getUiUserName().equals(Strings.nullToEmpty(server.getUserName())))
            return true;
        if (!getUiPassword().equals(Strings.nullToEmpty(server.getPassword())))
            return true;
        if (!getUiRpcUrl().equals(server.getRpcUrl()))
            return true;
        return false;
    }

    private void updateUI(Server server) {
        if (server == null) {
            serverNameEdit.setText("");
            hostNameEdit.setText("");
            portNumberEdit.setText(String.valueOf(DEFAULT_PORT));
            updateAuth(false);
            userNameEdit.setText("");
            passwordEdit.setText("");
            rpcUrlEdit.setText(Server.DEFAULT_RPC_URL);
        } else {
            serverNameEdit.setText(server.getName());
            hostNameEdit.setText(server.getHost());
            portNumberEdit.setText(String.valueOf(server.getPort()));
            updateAuth(server.isAuthenticationEnabled());
            userNameEdit.setText(server.getUserName());
            passwordEdit.setText(server.getPassword());
            rpcUrlEdit.setText(server.getRpcUrl());
        }
    }

    private String getUiName() {
        return serverNameEdit.getText().toString();
    }

    private String getUiHost() {
        return hostNameEdit.getText().toString();
    }

    private int getUiPort() {
        return Integer.parseInt(portNumberEdit.getText().toString());
    }

    private String getUiUserName() {
        return userNameEdit.getText().toString();
    }

    private String getUiPassword() {
        return passwordEdit.getText().toString();
    }

    private String getUiRpcUrl() {
        return StringUtils.stripEnd(StringUtils.stripStart(rpcUrlEdit.getText().toString(), CHARS_TO_STRIP_RPC), CHARS_TO_STRIP_RPC);
    }

    private boolean checkValidity() {
        if (serverNameEdit.getText().length() == 0) {
            serverNameEdit.setError(getString(R.string.server_name_error_message));
            return false;
        }
        if (hostNameEdit.getText().length() == 0) {
            hostNameEdit.setError(getString(R.string.host_name_error_message));
            return false;
        }
        if (portNumberEdit.getText().length() == 0) {
            portNumberEdit.setError(getString(R.string.port_number_error_message));
            return false;
        }

        return true;
    }

    private void updateAuth(boolean isEnabled) {
        isAuthEnabled = isEnabled;
        authCheckBox.setChecked(isEnabled);
        userNameEdit.setEnabled(isEnabled);
        passwordEdit.setEnabled(isEnabled);
    }

    private static class ClearErrorTextWatcher implements TextWatcher {

        private EditText editText;

        public ClearErrorTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            editText.setError(null);
        }
    }
}
