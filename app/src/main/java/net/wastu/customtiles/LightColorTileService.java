package net.wastu.customtiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LightColorTileService extends TileService {
    private static final String TAG = "LightColorTileService";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private void updateTileState(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) return;

        int level = DisplayHardware.getTargetLightColorLevel(this);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_light_color));
        tile.setLabel(getString(R.string.tile_light_color_label));
        if (active) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle(level + "% Muted");
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
            boolean active = DisplayHardware.isLightColorActive(this);
            updateTileState(active);
        });
    }

    @Override
    public void onClick() {
        super.onClick();
        mExecutor.execute(() -> {
            boolean active = DisplayHardware.isLightColorActive(this);
            boolean target = !active;
            DisplayHardware.setLightColorEnabled(this, target);
            try {
                Thread.sleep(60);
            } catch (InterruptedException ignored) {}
            boolean confirmed = DisplayHardware.isLightColorActive(this);
            updateTileState(confirmed);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}
