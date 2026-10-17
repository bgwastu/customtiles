package net.wastu.customtiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HbmTileService extends TileService {
    private static final String TAG = "HbmTileService";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private void updateTileState(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_hbm));
        tile.setLabel(getString(R.string.tile_hbm_label));
        if (active) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle(getString(R.string.tile_state_on_peak));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(getString(R.string.tile_state_off));
        }
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mExecutor.execute(() -> {
            boolean active = DisplayHardware.isHbmActive(this);
            updateTileState(active);
        });
    }

    @Override
    public void onClick() {
        super.onClick();
        mExecutor.execute(() -> {
            boolean active = DisplayHardware.isHbmActive(this);
            boolean target = !active;
            DisplayHardware.setHbmEnabled(this, target);
            try {
                Thread.sleep(80);
            } catch (InterruptedException ignored) {}
            boolean confirmed = DisplayHardware.isHbmActive(this);
            updateTileState(confirmed);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}
