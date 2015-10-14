package com.example.karhades_pc.tag_it;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.karhades_pc.contextual_action_bar.MaterialCab;
import com.example.karhades_pc.nfc.NfcHandler;
import com.example.karhades_pc.sliding_tab_layout.SlidingTabLayout;

/**
 * Created by Karhades on 20-Aug-15.
 */
public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private Toolbar toolbar;
    private SlidingTabLayout slidingTabLayout;
    private FrameLayout statusBar;
    private MaterialCab materialCab;

    private NfcHandler nfcHandler;
    private int pagePosition = 0;

    private static OnContextActivityListener onContextActivityListener;

    public static void setOnContextActivityListener(OnContextActivityListener newOnContextActivityListener) {
        onContextActivityListener = newOnContextActivityListener;
    }

    public interface OnContextActivityListener {
        void onMenuItemPressed(int id);

        void onContextExited();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        statusBar = (FrameLayout) findViewById(R.id.main_status_bar);

        setupNFC();
        setupToolbar();
        setupTabMenu();
        setupContextualActionBar();
        setUpNavigationDrawer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Save the tags to a file.
        MyTags.get(this).saveTags();

        nfcHandler.disableForegroundDispatch();

        // If Contextual Action Bar is enabled, close it.
        if (materialCab != null && materialCab.isActive()) {
            disableContextBar();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (pagePosition == 0) {
            nfcHandler.enableNfcReadTag(intent);
        } else if (pagePosition == 1) {
            Toast.makeText(this, "Share game tab!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Click the + button or overwrite an existing NFC tag.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        nfcHandler.enableForegroundDispatch();
    }

    @Override
    public void onBackPressed() {
        if (materialCab != null && materialCab.isActive()) {
            disableContextBar();
        } else {
            super.onBackPressed();
        }
    }

    private void disableContextBar() {
        materialCab.finish();
        onContextActivityListener.onContextExited();
    }

    private void setupNFC() {
        nfcHandler = new NfcHandler();
        nfcHandler.setupNfcHandler(this);
        nfcHandler.enableNfcReadTag(getIntent());
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.main_tool_bar);
        // Substitute the action bar for this toolbar.
        setSupportActionBar(toolbar);
    }

    private void setupTabMenu() {
        // Tab names.
        final String[] tabNames = {"TRACKING", "SHARE", "CREATE"};

        // The FragmentManager is needed for the view pager.
        FragmentManager fragmentManager = getSupportFragmentManager();

        viewPager = (ViewPager) findViewById(R.id.main_view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new TrackingGameFragment();
                    case 1:
                        return new ShareGameFragment();
                    case 2:
                        return new CreateGameFragment();
                }

                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return tabNames[position];
            }
        });
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tab_layout);
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setViewPager(viewPager);
        // Slider color.
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }
        });
        slidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // DO NOTHING.
            }

            @Override
            public void onPageSelected(int position) {
                pagePosition = position;
                if (materialCab != null && materialCab.isActive() && position != 2) {
                    disableContextBar();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // DO NOTHING.
            }
        });
    }

    private void setupContextualActionBar() {
        materialCab = new MaterialCab(this, R.id.view_stub);
        materialCab.setMenu(R.menu.fragment_create_game_context);
        materialCab.setBackgroundColor(getResources().getColor(R.color.accent));

        // Listen for CreateGameFragment's events.
        CreateGameFragment.setOnContextFragmentListener(new CreateGameFragment.OnContextFragmentListener() {
            @Override
            public void onItemLongClicked() {
                changeBarThemeColor(false);

                materialCab.start(new MaterialCab.Callback() {
                    @Override
                    public boolean onCabCreated(MaterialCab cab, Menu menu) {
                        return true;
                    }

                    @Override
                    public boolean onCabItemClicked(MenuItem item) {
                        MenuItem selectAll = materialCab.getMenu().getItem(1);
                        MenuItem clearSelection = materialCab.getMenu().getItem(2);

                        switch (item.getItemId()) {
                            case R.id.context_bar_delete_item:
                                onContextActivityListener.onMenuItemPressed(R.id.context_bar_delete_item);
                                disableContextBar();
                                return true;
                            case R.id.context_bar_select_all_item:
                                clearSelection.setVisible(true);
                                selectAll.setVisible(false);
                                onContextActivityListener.onMenuItemPressed(R.id.context_bar_select_all_item);
                                return true;
                            case R.id.context_bar_clear_selection_item:
                                clearSelection.setVisible(false);
                                selectAll.setVisible(true);
                                onContextActivityListener.onMenuItemPressed(R.id.context_bar_clear_selection_item);
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public boolean onCabFinished(MaterialCab cab) {
                        changeBarThemeColor(true);
                        onContextActivityListener.onContextExited();
                        return true;
                    }
                });
            }

            @Override
            public void onItemClicked(int tagsSelected) {
                materialCab.setTitle(tagsSelected + " selected");
            }
        });
    }

    private void changeBarThemeColor(boolean isVisible) {
        if (isVisible) {
            statusBar.setBackgroundColor(getResources().getColor(R.color.primary_dark));

            slidingTabLayout.setTextViewActivated(false, 2);
            slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.primary));
            slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
                @Override
                public int getIndicatorColor(int position) {
                    return getResources().getColor(R.color.accent);
                }
            });
        } else {
            statusBar.setBackgroundColor(getResources().getColor(R.color.accent_dark));

            slidingTabLayout.setTextViewActivated(true, 2);
            slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.accent));
            slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
                @Override
                public int getIndicatorColor(int position) {
                    return getResources().getColor(R.color.primary);
                }
            });
        }
    }

    private void setUpNavigationDrawer() {
        NavigationDrawerFragment fragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer_fragment);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer_layout);
        fragment.setUp(drawerLayout, toolbar);
    }
}