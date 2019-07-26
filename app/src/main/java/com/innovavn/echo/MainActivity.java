package com.innovavn.echo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.innovavn.echo.action.ActionsFragment;
import com.innovavn.echo.action.BaseListenerFragment;

import static com.innovavn.echo.R.id.frame;

public class MainActivity extends BaseActivity implements ActionsFragment.ActionFragmentInterface, FragmentManager.OnBackStackChangedListener{
    private static final String TAG = "MainActivity";
    private final static String TAG_FRAGMENT = "CurrentFragment";

    private TextView status;
    private View loading;
    private View statusBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Listen for changes in the backstack
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        shouldDisplayHomeUp();

        statusBar = findViewById(R.id.status_bar);
        status = (TextView) findViewById(R.id.status);
        loading = findViewById(R.id.loading);

        ActionsFragment fragment = new ActionsFragment();
        loadFragment(fragment, false);
    }

    protected void startListening(){
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        if (fragment != null && fragment.isVisible()) {
            // add your code here
            if (fragment instanceof BaseListenerFragment) {
                ((BaseListenerFragment) fragment).startListening();
            }
        }
    }

    @Override
    public void loadFragment(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(frame, fragment, TAG_FRAGMENT);
        if (addToBackstack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }

    protected void stateListening(){
        if(status != null) {
            status.setText(R.string.status_listening);
            loading.setVisibility(View.GONE);
            statusBar.animate().alpha(1);
        }
    }

    protected void stateProcessing() {
        if(status != null) {
            status.setText(R.string.status_processing);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }

    protected void stateSpeaking(){
        if(status != null) {
            status.setText(R.string.status_speaking);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }

    protected void statePrompting(){
        if(status != null) {
            status.setText("");
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }

    protected void stateFinished(){
        if(status != null) {
            status.setText("");
            loading.setVisibility(View.GONE);
            statusBar.animate().alpha(0);
        }
    }

    protected void stateNone(){
        statusBar.animate().alpha(0);
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    private void shouldDisplayHomeUp() {
        // Enable Up button only if there are entries in the back stack
        boolean blIsCanBack = (getSupportFragmentManager().getBackStackEntryCount() > 0);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(blIsCanBack);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // This method is called when the up button is pressed Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }
}
