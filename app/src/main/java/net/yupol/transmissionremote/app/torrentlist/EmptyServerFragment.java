package net.yupol.transmissionremote.app.torrentlist;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import net.yupol.transmissionremote.app.R;
import net.yupol.transmissionremote.app.utils.ColorUtils;

public class EmptyServerFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.empty_server_layout, container, false);

        Button addBtn = (Button) view.findViewById(R.id.add_server_button);
        int iconColor = ColorUtils.resolveColor(getContext(), android.R.attr.textColorSecondary, R.color.text_secondary);
        addBtn.setCompoundDrawables(
                new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_add).color(iconColor).sizeRes(R.dimen.default_button_icon_size),
                null, null, null);

        return view;
    }
}
