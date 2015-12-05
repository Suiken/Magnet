package kei.magnet.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import kei.magnet.R;
import kei.magnet.classes.ApplicationUser;
import kei.magnet.classes.Group;
import kei.magnet.classes.Location;
import kei.magnet.classes.User;
import kei.magnet.enumerations.NavigationDrawerType;
import kei.magnet.fragments.AddUserFragment;
import kei.magnet.fragments.CustomDrawerAdapter;
import kei.magnet.fragments.DrawerItem;
import kei.magnet.utils.Compass;
import kei.magnet.utils.GPSHandler;

public class MagnetActivity extends AppCompatActivity {
    private boolean isInitialised = false;
    private DrawerLayout menuLayout;
    private ActionBarDrawerToggle actionBarButtonLink;
    public GPSHandler gpsHandler;
    private Compass compass;
    private SensorManager sensorManager;
    private ApplicationUser applicationUser;
    private ListView menuList;
    private CustomDrawerAdapter customDrawerAdapter;
    private List<DrawerItem> menuDataList;
    public static Group selectedGroup;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnet);
        applicationUser = ApplicationUser.getInstance();

        if (applicationUser.getToken() == null) {
            Intent signInIntent = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(signInIntent);
        } else {
            init();
        }
    }

    public List<DrawerItem> formatGroupsInDataList(List<Group> groups) {
        menuDataList = new ArrayList<>();

        Group globalGroup = new Group();
        globalGroup.setCreator(ApplicationUser.getInstance());
        globalGroup.setName("Friend list");
        List<User> groupZeroUsers = new ArrayList<>();

        for (Group group : groups) {
            menuDataList.add(new DrawerItem(group, NavigationDrawerType.GROUP));
            for (User user : group.getUsers()) {
                menuDataList.add(new DrawerItem(user, NavigationDrawerType.USER));
                if (!groupZeroUsers.contains(user)) {
                    groupZeroUsers.add(user);
                }
            }
        }

        globalGroup.setUsers(groupZeroUsers);
        menuDataList.add(new DrawerItem(globalGroup, NavigationDrawerType.GROUP));
        for (User user : globalGroup.getUsers()) {
            menuDataList.add(new DrawerItem(user, NavigationDrawerType.USER));
        }

        return menuDataList;
    }


    public void init() {
        isInitialised = true;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        compass = new Compass(sensorManager, this);

        try {
            gpsHandler = new GPSHandler(this, applicationUser);
            gpsHandler.getGoogleMap().setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    Intent intent = new Intent(getApplicationContext(), PinCreationActivity.class);
                    intent.putExtra("location", new Location(latLng));
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMenu();
    }

    public void initMenu() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        menuLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        menuLayout.setDrawerListener(actionBarButtonLink);
        menuLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        menuList = (ListView) findViewById(R.id.left_drawer);

        actionBarButtonLink = new ActionBarDrawerToggle(this, menuLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        menuDataList = formatGroupsInDataList(applicationUser.getGroups());

        customDrawerAdapter = new CustomDrawerAdapter(this, R.layout.custom_drawer_item,
                menuDataList);
        menuList.setAdapter(customDrawerAdapter);

        menuList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (menuDataList.get(position).getItem() instanceof Group) {
                    selectedGroup = (Group) menuDataList.get(position).getItem();
                    view.setSelected(true);
                    gpsHandler.updateMarkers(selectedGroup);
                }
                if (menuDataList.get(position).getItem() instanceof User) {
                    User selectedUser = (User) menuDataList.get(position).getItem();
                    gpsHandler.moveCamera(selectedUser.getLatLng(), 10);
                }
            }
        });

        menuList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (menuDataList.get(position).getItem() instanceof Group) {
                    selectedGroup = (Group) menuDataList.get(position).getItem();

                    AddUserFragment dialog = new AddUserFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("group", selectedGroup);
                    dialog.setArguments(bundle);
                    dialog.show(getFragmentManager(), "Add user");
                    return true;
                } else {
                    Toast.makeText(getApplicationContext(), "Not a valid Group", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isInitialised) {
            gpsHandler.onPause();
            compass.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInitialised) {
            gpsHandler.onResume();
            compass.onResume();
        } else if (applicationUser.getToken() != null) {
            init();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isInitialised) {
            gpsHandler.onResume();
            compass.onResume();
        } else if (applicationUser.getToken() != null) {
            init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (isInitialised)
            actionBarButtonLink.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isInitialised)
            actionBarButtonLink.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarButtonLink.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public List<DrawerItem> getMenuDataList() {
        return menuDataList;
    }
}

