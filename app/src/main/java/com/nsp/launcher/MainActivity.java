package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "nsp";

    private static final int C_BG_DARK = 0xFF000000;
    private static final int C_BG_LIGHT = 0xFFFFFFFF;
    private static final int C_TEXT_DARK = 0xFFFFFFFF;
    private static final int C_TEXT_LIGHT = 0xFF111111;
    private static final int C_SEC_DARK = 0xFF9E9E9E;
    private static final int C_SEC_LIGHT = 0xFF666666;
    private static final int C_ACCENT = 0xFF1BA1E2;
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int COLUMNS = 5;
    private static final int TILE_DP = 104;

    private SharedPreferences prefs;
    private boolean isLight;
    private boolean bgEnabled;
    private AppRepository repo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private LinearLayout content;
    private final ArrayList<Section> sections = new ArrayList<Section>();
    private ArrayList<AppEntry> apps = new ArrayList<AppEntry>();

    private int focusSection = 0;
    private int focusIndex = 0;

    private boolean dialogOpen = false;
    private boolean scanning = false;
    private int stickDir = 0;
    private long lastStickTime = 0;
    private String pendingFocus = null;

    private Dashboard dashboard;
    private WeatherData weather;
    private boolean resumed = false;
    private boolean weatherLoading = false;
    private boolean recentsDirty = false;
    private BroadcastReceiver packageReceiver;
    private IconStore iconStore;
    private Bitmap customBackgroundBitmap;

    private final Runnable packageReload = new Runnable() {
        public void run() {
            if (!scanning && !dialogOpen) scanApps(false);
        }
    };

    private static class Section {
        String title;
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        int lastIndex = 0;
        LinearLayout container;
        LinearLayout row;
        LinearLayout grid;
        TextView header;
    }

    private static class Tile {
        AppEntry entry;
        Runnable action;
        View view;
        ImageView icon;
        TextView label;
        TextView badge;
        GradientDrawable normal;
        GradientDrawable focused;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean effectiveLight = BackgroundManager.effectiveLight(this);
        if (prefs.getBoolean("light", false) != effectiveLight) {
            prefs.edit().putBoolean("light", effectiveLight).apply();
        }
        isLight = effectiveLight;
        bgEnabled = prefs.getBoolean("background", true);
        setTheme(isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        repo = new AppRepository(this);
        iconStore = new IconStore(this);
        weather = WeatherService.loadCached(this);
        if (savedInstanceState != null) {
            pendingFocus = savedInstanceState.getString("focusPkg");
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(bgColor());
        root.setClipChildren(false);
        setContentView(root);

        applyImmersive();

        ArrayList<AppEntry> cached = repo.readCache();
        if (!cached.isEmpty()) {
            apps = cached;
            String focus = pendingFocus;
            pendingFocus = null;
            buildUi(cached, focus);
            scanApps(false);
        } else {
            showLoading();
            scanApps(true);
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private int bgColor() {
        return isLight ? C_BG_LIGHT : C_BG_DARK;
    }

    private int textColor() {
        return isLight ? C_TEXT_LIGHT : C_TEXT_DARK;
    }

    private int secondaryColor() {
        return isLight ? C_SEC_LIGHT : C_SEC_DARK;
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    private void showLoading() {
        root.removeAllViews();
        TextView t = new TextView(this);
        t.setText("CARGANDO APLICACIONES");
        t.setTextSize(13);
        t.setLetterSpacing(0.25f);
        t.setTextColor(C_ACCENT);
        t.setGravity(Gravity.CENTER);
        root.addView(t, new FrameLayout.LayoutParams(-1, -1));
    }

    private void scanApps(final boolean initial) {
        if (scanning) return;
        scanning = true;
        new Thread(new Runnable() {
            public void run() {
                final ArrayList<AppEntry> list = repo.load();
                ui.post(new Runnable() {
                    public void run() {
                        scanning = false;
                        applyScan(list, initial);
                    }
                });
            }
        }).start();
    }

    private void applyScan(ArrayList<AppEntry> list, boolean initial) {
        for (AppEntry e : list) {
            AppEntry old = findApp(apps, e.pkg);
            if (old != null && old.updated != e.updated) {
                iconStore.invalidate(e.pkg);
            }
        }

        repo.writeCache(list);

        boolean changed = !sameApps(apps, list);
        if (changed || apps.isEmpty()) {
            String focus = focusedPackage();
            if (focus == null) focus = pendingFocus;
            pendingFocus = null;
            buildUi(list, focus);
        }
    }

    private AppEntry findApp(ArrayList<AppEntry> list, String pkg) {
        for (AppEntry e : list) {
            if (e.pkg.equals(pkg)) return e;
        }
        return null;
    }

    private boolean sameApps(ArrayList<AppEntry> a, ArrayList<AppEntry> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            AppEntry x = a.get(i);
            AppEntry y = b.get(i);
            if (!x.pkg.equals(y.pkg) || !x.activity.equals(y.activity)
                    || !x.label.equals(y.label) || x.updated != y.updated) {
                return false;
            }
        }
        return true;
    }

    private void reloadApps() {
        if (scanning) return;
        scanApps(false);
    }

    private void buildUi(ArrayList<AppEntry> list, final String focusPkg) {
        apps = list;
        sections.clear();
        if (dashboard != null) {
            dashboard.stop();
            dashboard = null;
        }
        root.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(24), dp(8), dp(24), dp(4));

        TextView title = new TextView(this);
        title.setText("NSP");
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setTextSize(30);
        title.setLetterSpacing(0.04f);
        title.setTextColor(textColor());
        title.setSingleLine(true);
        header.addView(title, new LinearLayout.LayoutParams(-2, -2));

        TextView hint = new TextView(this);
        hint.setText("A abrir  |  X favorito  |  Y mover  |  SELECT tema  |  START recargar");
        hint.setTextSize(9);
        hint.setTextColor(secondaryColor());
        hint.setSingleLine(true);
        hint.setEllipsize(TextUtils.TruncateAt.END);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(hint, new LinearLayout.LayoutParams(0, -2, 1f));

        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new LinearLayout.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        if (bgEnabled) {
            ImageView background = new ImageView(this);
            if (BackgroundManager.hasCustom(this)) {
                if (customBackgroundBitmap == null) {
                    customBackgroundBitmap = BackgroundManager.load(this);
                }
                if (customBackgroundBitmap != null) {
                    background.setImageBitmap(customBackgroundBitmap);
                } else {
                    background.setImageResource(isLight ? R.drawable.bg_light : R.drawable.bg_dark);
                }
            } else {
                background.setImageResource(isLight ? R.drawable.bg_light : R.drawable.bg_dark);
            }
            background.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(background, new FrameLayout.LayoutParams(-1, -1));

            View scrim = new View(this);
            scrim.setBackgroundColor(isLight ? 0xB3FFFFFF : 0x99000000);
            root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        }

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        addSystemSection();
        addRecentSection();

        ArrayList<AppEntry> favs = new ArrayList<AppEntry>();
        for (AppEntry e : apps) {
            if (repo.isFavorite(e.pkg)) favs.add(e);
        }
        addSection(AppRepository.FAV, favs);

        for (String cat : AppRepository.CATS) {
            ArrayList<AppEntry> catApps = new ArrayList<AppEntry>();
            for (AppEntry e : apps) {
                if (cat.equals(e.category)) catApps.add(e);
            }
            addSection(cat, catApps);
        }

        root.post(new Runnable() {
            public void run() {
                boolean restored = false;
                if (focusPkg != null) restored = focusByPackage(focusPkg);
                if (!restored) focusFirstApp();
            }
        });

        if (dashboard != null) {
            dashboard.setSecondsEnabled(prefs.getBoolean("clock_seconds", false));
            dashboard.setWeather(weather);
            dashboard.refreshNow();
            if (resumed) dashboard.start();
        }
        maybeRefreshWeather();
    }

    private void addSystemSection() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText("SISTEMA");
        header.setTextSize(12);
        header.setLetterSpacing(0.2f);
        header.setTextColor(secondaryColor());
        header.setPadding(dp(24), dp(14), dp(24), dp(2));
        container.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(20), dp(4), dp(24), dp(12));

        Section section = new Section();
        section.title = "Sistema";

        Tile tile = makeSystemTile("Ajustes", R.mipmap.ic_settings, 0xFF647687,
                new Runnable() {
                    public void run() {
                        openSettings();
                    }
                });
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(96), dp(96));
        tileParams.rightMargin = dp(6);
        row.addView(tile.view, tileParams);
        section.tiles.add(tile);

        dashboard = new Dashboard(this, isLight, prefs.getBoolean("clock_seconds", false),
                new Runnable() {
                    public void run() {
                        fetchWeather(true);
                    }
                });
        row.addView(dashboard, new LinearLayout.LayoutParams(0, dp(96), 1f));

        container.addView(row, new LinearLayout.LayoutParams(-1, -2));
        content.addView(container, new LinearLayout.LayoutParams(-1, -2));

        section.container = container;
        section.header = header;
        section.row = row;
        sections.add(section);
    }

    private Section createAppSection(String title) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText(title.toUpperCase(new Locale("es", "ES")));
        header.setTextSize(12);
        header.setLetterSpacing(0.2f);
        header.setTextColor(secondaryColor());
        header.setPadding(dp(24), dp(14), dp(24), dp(2));
        container.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(dp(20), dp(4), dp(24), dp(12));
        container.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        Section section = new Section();
        section.title = title;
        section.container = container;
        section.header = header;
        section.grid = grid;
        return section;
    }

    private void insertSection(Section section, int index) {
        if (index < 0) index = 0;
        if (index > sections.size()) index = sections.size();
        content.addView(section.container, index);
        sections.add(index, section);
    }

    private void addSection(String title, ArrayList<AppEntry> list) {
        if (list.isEmpty()) return;
        Section section = createAppSection(title);
        int color = AppRepository.colorFor(title);
        for (AppEntry e : list) {
            section.tiles.add(makeTile(e, color));
        }
        buildGrid(section);
        insertSection(section, sections.size());
    }

    private void addRecentSection() {
        ArrayList<AppEntry> recents = resolveRecents();
        if (recents.isEmpty()) return;
        Section section = createAppSection(AppRepository.RECENT);
        int color = AppRepository.colorFor(AppRepository.RECENT);
        for (AppEntry entry : recents) {
            section.tiles.add(makeTile(entry, color));
        }
        buildGrid(section);
        insertSection(section, 1);
    }

    private ArrayList<AppEntry> resolveRecents() {
        ArrayList<AppEntry> out = new ArrayList<AppEntry>();
        ArrayList<String> packages = repo.recentPackages();
        for (String pkg : packages) {
            AppEntry entry = findApp(apps, pkg);
            if (entry != null) out.add(entry);
            if (out.size() >= 8) break;
        }
        return out;
    }

    private void buildGrid(Section section) {
        if (section.grid == null) return;
        for (Tile tile : section.tiles) {
            ViewParent parent = tile.view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(tile.view);
            }
        }
        section.grid.removeAllViews();
        for (int i = 0; i < section.tiles.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            for (int j = i; j < section.tiles.size() && j < i + COLUMNS; j++) {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(TILE_DP), dp(TILE_DP));
                params.rightMargin = dp(8);
                row.addView(section.tiles.get(j).view, params);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            section.grid.addView(row, rowParams);
        }
    }

    private void insertTile(Section section, AppEntry entry, boolean append) {
        int color = AppRepository.colorFor(section.title);
        Tile tile = makeTile(entry, color);
        if (append) {
            section.tiles.add(tile);
        } else {
            int position = section.tiles.size();
            for (int i = 0; i < section.tiles.size(); i++) {
                if (entry.label.compareToIgnoreCase(section.tiles.get(i).entry.label) < 0) {
                    position = i;
                    break;
                }
            }
            section.tiles.add(position, tile);
        }
        buildGrid(section);
    }

    private Section findSection(String title) {
        for (Section section : sections) {
            if (section.title.equals(title)) return section;
        }
        return null;
    }

    private Tile findTile(Section section, String pkg) {
        for (Tile tile : section.tiles) {
            if (tile.entry != null && tile.entry.pkg.equals(pkg)) return tile;
        }
        return null;
    }

    private void removeTile(Section section, Tile tile) {
        section.tiles.remove(tile);
        if (section.tiles.isEmpty()) {
            removeSection(section);
        } else {
            buildGrid(section);
        }
    }

    private void removeSection(Section section) {
        int index = sections.indexOf(section);
        if (index < 0) return;
        content.removeView(section.container);
        sections.remove(index);
        if (focusSection > index) {
            focusSection--;
        } else if (focusSection == index) {
            focusSection = Math.max(0, index - 1);
        }
    }

    private void restoreFocus(String pkg) {
        if (pkg != null && focusByPackage(pkg)) return;
        focusFirstApp();
    }

    private void addFavoriteInPlace(AppEntry entry) {
        Section favorites = findSection(AppRepository.FAV);
        if (favorites == null) {
            favorites = createAppSection(AppRepository.FAV);
            int position = 1;
            if (findSection(AppRepository.RECENT) != null) position = 2;
            insertSection(favorites, position);
        }
        if (findTile(favorites, entry.pkg) == null) {
            insertTile(favorites, entry, false);
        }
    }

    private void removeFavoriteInPlace(AppEntry entry) {
        Section favorites = findSection(AppRepository.FAV);
        if (favorites == null) return;
        Tile tile = findTile(favorites, entry.pkg);
        if (tile != null) removeTile(favorites, tile);
    }

    private int categoryRank(String title) {
        for (int i = 0; i < AppRepository.CATS.length; i++) {
            if (AppRepository.CATS[i].equals(title)) return i;
        }
        return -1;
    }

    private int categoryInsertPosition(String category) {
        int rank = categoryRank(category);
        for (int i = 0; i < sections.size(); i++) {
            int other = categoryRank(sections.get(i).title);
            if (other >= 0 && other > rank) return i;
        }
        return sections.size();
    }

    private void moveCategoryInPlace(AppEntry entry, String oldCategory, String newCategory) {
        Section oldSection = findSection(oldCategory);
        if (oldSection != null) {
            Tile oldTile = findTile(oldSection, entry.pkg);
            if (oldTile != null) removeTile(oldSection, oldTile);
        }
        Section target = findSection(newCategory);
        if (target == null) {
            target = createAppSection(newCategory);
            insertSection(target, categoryInsertPosition(newCategory));
        }
        if (findTile(target, entry.pkg) == null) {
            insertTile(target, entry, false);
        }
    }

    private void refreshRecents() {
        recentsDirty = false;
        if (apps.isEmpty()) return;
        String focused = focusedPackage();
        Section old = findSection(AppRepository.RECENT);
        if (old != null) removeSection(old);
        ArrayList<AppEntry> recents = resolveRecents();
        if (!recents.isEmpty()) {
            Section section = createAppSection(AppRepository.RECENT);
            int color = AppRepository.colorFor(AppRepository.RECENT);
            for (AppEntry entry : recents) {
                section.tiles.add(makeTile(entry, color));
            }
            buildGrid(section);
            insertSection(section, 1);
        }
        restoreFocus(focused);
    }

    private Tile makeTile(AppEntry e, int color) {
        final Tile t = new Tile();
        t.entry = e;

        t.normal = new GradientDrawable();
        t.normal.setShape(GradientDrawable.RECTANGLE);
        t.normal.setColor(color);

        t.focused = new GradientDrawable();
        t.focused.setShape(GradientDrawable.RECTANGLE);
        t.focused.setColor(color);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackgroundDrawable(t.normal);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(42), dp(42));
        iconLp.gravity = Gravity.TOP | Gravity.LEFT;
        iconLp.leftMargin = dp(11);
        iconLp.topMargin = dp(10);
        tile.addView(icon, iconLp);

        TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextSize(11);
        label.setTextColor(C_WHITE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(-1, -2);
        labelLp.gravity = Gravity.BOTTOM;
        labelLp.leftMargin = dp(9);
        labelLp.rightMargin = dp(9);
        labelLp.bottomMargin = dp(8);
        tile.addView(label, labelLp);

        TextView badge = new TextView(this);
        badge.setTextSize(9);
        badge.setTextColor(C_WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(0xFFE51400);
        badge.setBackgroundDrawable(bubble);
        badge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(20), dp(20));
        badgeLp.gravity = Gravity.TOP | Gravity.RIGHT;
        badgeLp.topMargin = dp(6);
        badgeLp.rightMargin = dp(6);
        tile.addView(badge, badgeLp);

        t.view = tile;
        t.icon = icon;
        t.label = label;
        t.badge = badge;

        tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(t.entry);
            }
        });

        tile.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showCategoryDialog(t.entry);
                return true;
            }
        });

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundDrawable(t.focused);
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start();
                } else {
                    v.setBackgroundDrawable(t.normal);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                }
            }
        });

        Bitmap cachedIcon = iconStore.get(e.pkg);
        if (cachedIcon != null) icon.setImageBitmap(cachedIcon);
        applyBadge(t);
        requestIcon(e);
        return t;
    }

    private void applyBadge(Tile tile) {
        if (tile == null || tile.badge == null) return;
        int count = tile.entry == null ? 0 : NotificationStore.count(tile.entry.pkg);
        if (count > 0) {
            tile.badge.setText(count > 99 ? "99+" : String.valueOf(count));
            tile.badge.setVisibility(View.VISIBLE);
        } else {
            tile.badge.setVisibility(View.GONE);
        }
    }

    private void updateBadges(String pkg) {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                if (tile.entry != null && tile.entry.pkg.equals(pkg)) {
                    applyBadge(tile);
                }
            }
        }
    }

    private void applyAllBadges() {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                applyBadge(tile);
            }
        }
    }

    private final NotificationStore.Listener badgeListener = new NotificationStore.Listener() {
        public void onBadgeChanged(String pkg, int count) {
            updateBadges(pkg);
        }
    };

    private void requestIcon(final AppEntry entry) {
        Bitmap cached = iconStore.get(entry.pkg);
        if (cached != null) {
            updateTilesIcon(entry.pkg, cached, entry.updated);
            return;
        }
        final long updated = entry.updated;
        final String pkg = entry.pkg;
        iconStore.request(pkg, entry.activity, updated, new IconStore.Listener() {
            public void onIcon(String name, Bitmap bitmap) {
                updateTilesIcon(name, bitmap, updated);
            }
        });
    }

    private void updateTilesIcon(String pkg, Bitmap bitmap, long updated) {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                if (tile.entry != null && tile.entry.pkg.equals(pkg)
                        && tile.entry.updated == updated) {
                    tile.icon.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Tile makeSystemTile(String name, int iconRes, int color, final Runnable action) {
        final Tile t = new Tile();
        t.action = action;

        t.normal = new GradientDrawable();
        t.normal.setShape(GradientDrawable.RECTANGLE);
        t.normal.setColor(color);

        t.focused = new GradientDrawable();
        t.focused.setShape(GradientDrawable.RECTANGLE);
        t.focused.setColor(color);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackgroundDrawable(t.normal);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(iconRes);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(42), dp(42));
        iconLp.gravity = Gravity.TOP | Gravity.LEFT;
        iconLp.leftMargin = dp(11);
        iconLp.topMargin = dp(10);
        tile.addView(icon, iconLp);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextSize(11);
        label.setTextColor(C_WHITE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(-1, -2);
        labelLp.gravity = Gravity.BOTTOM;
        labelLp.leftMargin = dp(9);
        labelLp.rightMargin = dp(9);
        labelLp.bottomMargin = dp(8);
        tile.addView(label, labelLp);

        t.view = tile;
        t.icon = icon;
        t.label = label;

        tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (t.action != null) t.action.run();
            }
        });

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundDrawable(t.focused);
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start();
                } else {
                    v.setBackgroundDrawable(t.normal);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                }
            }
        });

        return t;
    }

    private void focusTile(int sectionIndex, int index) {
        if (sectionIndex < 0 || sectionIndex >= sections.size()) return;
        Section s = sections.get(sectionIndex);
        if (index < 0 || index >= s.tiles.size()) return;

        focusSection = sectionIndex;
        focusIndex = index;
        s.lastIndex = index;

        final View v = s.tiles.get(index).view;
        v.requestFocus();
        v.post(new Runnable() {
            public void run() {
                Rect r = new Rect(0, 0, Math.max(1, v.getWidth()), Math.max(1, v.getHeight()));
                v.requestRectangleOnScreen(r, true);
            }
        });
    }

    private boolean focusByPackage(String pkg) {
        for (int si = 0; si < sections.size(); si++) {
            Section s = sections.get(si);
            for (int ti = 0; ti < s.tiles.size(); ti++) {
                Tile tile = s.tiles.get(ti);
                if (tile.entry != null && tile.entry.pkg.equals(pkg)) {
                    focusTile(si, ti);
                    return true;
                }
            }
        }
        return false;
    }

    private void focusFirstApp() {
        for (int si = 0; si < sections.size(); si++) {
            Section s = sections.get(si);
            if ("Sistema".equals(s.title)) continue;
            if (!s.tiles.isEmpty()) {
                focusTile(si, 0);
                return;
            }
        }
        if (!sections.isEmpty()) focusTile(0, 0);
    }

    private String focusedPackage() {
        if (sections.isEmpty()) return null;
        if (focusSection >= sections.size()) return null;
        Section s = sections.get(focusSection);
        if (focusIndex >= s.tiles.size()) return null;
        Tile tile = s.tiles.get(focusIndex);
        return tile.entry == null ? null : tile.entry.pkg;
    }

    private Tile currentTile() {
        if (sections.isEmpty()) return null;
        if (focusSection < 0 || focusSection >= sections.size()) return null;
        Section s = sections.get(focusSection);
        if (focusIndex < 0 || focusIndex >= s.tiles.size()) return null;
        return s.tiles.get(focusIndex);
    }

    private void moveHorizontal(int direction) {
        if (sections.isEmpty()) return;
        Section current = sections.get(focusSection);
        if (current.tiles.isEmpty()) return;
        int column = focusIndex % COLUMNS;
        if (direction < 0 && column == 0) return;
        if (direction > 0 && column == COLUMNS - 1) return;
        int next = focusIndex + direction;
        if (next < 0 || next >= current.tiles.size()) return;
        focusTile(focusSection, next);
    }

    private void moveVertical(int direction) {
        if (sections.isEmpty()) return;
        Section current = sections.get(focusSection);
        if (current.tiles.isEmpty()) return;
        int column = focusIndex % COLUMNS;
        int next = focusIndex + direction * COLUMNS;
        if (next >= 0 && next < current.tiles.size()) {
            focusTile(focusSection, next);
            return;
        }
        moveSection(direction, column);
    }

    private void moveSection(int direction) {
        int column = 0;
        if (!sections.isEmpty() && focusSection < sections.size()) {
            Section current = sections.get(focusSection);
            if (!current.tiles.isEmpty()) column = focusIndex % COLUMNS;
        }
        moveSection(direction, column);
    }

    private void moveSection(int direction, int column) {
        int targetSection = focusSection + direction;
        if (targetSection < 0 || targetSection >= sections.size()) return;
        Section target = sections.get(targetSection);
        if (target.tiles.isEmpty()) return;
        int index;
        if (direction > 0) {
            int firstRow = Math.min(COLUMNS, target.tiles.size()) - 1;
            index = Math.min(column, firstRow);
        } else {
            int lastRowStart = ((target.tiles.size() - 1) / COLUMNS) * COLUMNS;
            index = Math.min(lastRowStart + column, target.tiles.size() - 1);
        }
        focusTile(targetSection, index);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dialogOpen) return super.dispatchKeyEvent(event);

        int code = event.getKeyCode();
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveHorizontal(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveHorizontal(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveVertical(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveVertical(1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    moveSection(-1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    moveSection(1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (event.getRepeatCount() == 0) openFocused();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_X:
                    if (event.getRepeatCount() == 0) toggleFavoriteFocused();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    if (event.getRepeatCount() == 0) categoryFocused();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                case KeyEvent.KEYCODE_MENU:
                    if (event.getRepeatCount() == 0) toggleTheme();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_START:
                    if (event.getRepeatCount() == 0) {
                        toast("Recargando aplicaciones");
                        reloadApps();
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_B:
                    if (event.getRepeatCount() == 0) {
                        if (!HomeUtil.isDefaultHome(this)) finish();
                    }
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (dialogOpen || sections.isEmpty()) return super.onGenericMotionEvent(event);

        int source = event.getSource();
        if ((source & InputDevice.SOURCE_JOYSTICK) == 0
                && (source & InputDevice.SOURCE_GAMEPAD) == 0
                && (source & InputDevice.SOURCE_DPAD) == 0) {
            return super.onGenericMotionEvent(event);
        }
        if (event.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event);
        }

        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        float x = Math.abs(hatX) > 0.5f ? hatX : event.getAxisValue(MotionEvent.AXIS_X);
        float y = Math.abs(hatY) > 0.5f ? hatY : event.getAxisValue(MotionEvent.AXIS_Y);

        int dir = 0;
        if (Math.abs(x) >= Math.abs(y) && Math.abs(x) > 0.55f) {
            dir = x > 0 ? 1 : 2;
        } else if (Math.abs(y) > 0.55f) {
            dir = y > 0 ? 3 : 4;
        }

        if (dir == 0) {
            stickDir = 0;
            return true;
        }

        long now = SystemClock.uptimeMillis();
        if (dir != stickDir || now - lastStickTime >= 140) {
            lastStickTime = now;
            stickDir = dir;
            if (dir == 1) moveHorizontal(1);
            else if (dir == 2) moveHorizontal(-1);
            else if (dir == 3) moveVertical(1);
            else moveVertical(-1);
        }
        return true;
    }

    private void openFocused() {
        Tile t = currentTile();
        if (t == null) return;
        if (t.entry != null) open(t.entry);
        else if (t.action != null) t.action.run();
    }

    private void open(AppEntry e) {
        repo.recordLaunch(e.pkg);
        recentsDirty = true;
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setClassName(e.pkg, e.activity);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception ex) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(e.pkg);
            if (launch != null) {
                try {
                    startActivity(launch);
                    return;
                } catch (Exception ignored) {
                }
            }
            toast("No se pudo abrir " + e.label);
        }
    }

    private void toggleFavoriteFocused() {
        Tile t = currentTile();
        if (t == null || t.entry == null) return;
        boolean added = repo.toggleFavorite(t.entry.pkg);
        if (added) {
            addFavoriteInPlace(t.entry);
        } else {
            removeFavoriteInPlace(t.entry);
        }
        toast(added ? "Añadido a Favoritos" : "Quitado de Favoritos");
        restoreFocus(t.entry.pkg);
    }

    private void categoryFocused() {
        Tile t = currentTile();
        if (t != null && t.entry != null) showCategoryDialog(t.entry);
    }

    private void showCategoryDialog(final AppEntry e) {
        final String[] items = new String[AppRepository.CATS.length + 1];
        System.arraycopy(AppRepository.CATS, 0, items, 0, AppRepository.CATS.length);
        items[items.length - 1] = "Automática (detectar)";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(e.label);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(textColor());
                tv.setTextSize(14);
                tv.setPadding(dp(20), dp(6), dp(20), dp(6));
                tv.setMinHeight(0);
                tv.setMinimumHeight(0);
                return tv;
            }
        };
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                String oldCategory = e.category;
                if (which == AppRepository.CATS.length) {
                    repo.clearCategory(e.pkg);
                } else {
                    repo.setCategory(e.pkg, AppRepository.CATS[which]);
                }
                String newCategory = repo.categoryOf(e);
                e.category = newCategory;
                if (!newCategory.equals(oldCategory)) {
                    moveCategoryInPlace(e, oldCategory, newCategory);
                }
                restoreFocus(e.pkg);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && event.getAction() == KeyEvent.ACTION_DOWN) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
                ui.postDelayed(new Runnable() {
                    public void run() {
                        restoreFocus(e.pkg);
                    }
                }, 60);
            }
        });
        dialogOpen = true;
        dialog.show();
    }

    private void toggleTheme() {
        isLight = !isLight;
        prefs.edit().putBoolean("light", isLight).apply();
        if (BackgroundManager.hasCustom(this)) {
            BackgroundManager.setStyle(this,
                    isLight ? BackgroundManager.STYLE_LIGHT : BackgroundManager.STYLE_DARK);
        }
        recreate();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void maybeRefreshWeather() {
        if (!resumed || weatherLoading) return;
        if (!WeatherService.isStale(weather)) return;
        fetchWeather(false);
    }

    private void fetchWeather(final boolean force) {
        if (weatherLoading) return;
        if (!force && !WeatherService.isStale(weather)) return;
        weatherLoading = true;
        if (weather == null && dashboard != null) dashboard.setWeatherLoading();
        WeatherService.fetch(this, new WeatherService.Callback() {
            public void onResult(WeatherData data, String error) {
                weatherLoading = false;
                if (data != null) {
                    weather = data;
                    if (dashboard != null) dashboard.setWeather(data);
                } else if (weather == null && dashboard != null) {
                    dashboard.setWeatherError();
                }
                if (force) {
                    toast(data != null ? "Clima actualizado" : "No se pudo actualizar el clima");
                }
            }
        });
    }

    private void refreshIfPackagesChanged() {
        if (scanning || dialogOpen || apps.isEmpty()) return;
        Set<String> installed = repo.installedPackages();
        if (installed.size() != apps.size()) {
            reloadApps();
            return;
        }
        for (AppEntry e : apps) {
            if (!installed.contains(e.pkg)) {
                reloadApps();
                return;
            }
        }
    }

    private void registerPackageReceiver() {
        if (packageReceiver != null) return;
        packageReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (scanning || dialogOpen) return;
                ui.removeCallbacks(packageReload);
                ui.postDelayed(packageReload, 800);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
    }

    private void unregisterPackageReceiver() {
        if (packageReceiver == null) return;
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        packageReceiver = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;

        if (prefs.getBoolean("bg_changed", false)) {
            prefs.edit().putBoolean("bg_changed", false).apply();
            ui.post(new Runnable() {
                public void run() {
                    recreate();
                }
            });
            return;
        }

        boolean prefLight = prefs.getBoolean("light", false);
        boolean prefBackground = prefs.getBoolean("background", true);
        if (prefLight != isLight || prefBackground != bgEnabled) {
            ui.post(new Runnable() {
                public void run() {
                    recreate();
                }
            });
            return;
        }

        if (dashboard != null) {
            dashboard.setSecondsEnabled(prefs.getBoolean("clock_seconds", false));
            WeatherData cached = WeatherService.loadCached(this);
            if (cached != null && (weather == null || cached.updated > weather.updated)) {
                weather = cached;
                dashboard.setWeather(cached);
            }
            dashboard.start();
            dashboard.refreshNow();
        }

        if (recentsDirty) refreshRecents();

        NotificationStore.addListener(badgeListener);
        applyAllBadges();

        if (prefs.getBoolean("reload_apps", false)) {
            prefs.edit().putBoolean("reload_apps", false).apply();
            reloadApps();
        } else {
            refreshIfPackagesChanged();
        }

        maybeRefreshWeather();
        registerPackageReceiver();
    }

    @Override
    protected void onPause() {
        resumed = false;
        NotificationStore.removeListener(badgeListener);
        if (dashboard != null) dashboard.stop();
        unregisterPackageReceiver();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (HomeUtil.isDefaultHome(this)) return;
        super.onBackPressed();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String pkg = focusedPackage();
        if (pkg != null) outState.putString("focusPkg", pkg);
    }
}


