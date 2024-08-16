package com.zhizi42.diymiuicard;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> {
                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        setTitle(R.string.title_activity_settings);
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);

            SwitchPreference hideIconPreference = findPreference("hide_icon");
            if (hideIconPreference != null) {
                hideIconPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean hide = (boolean) newValue;
                    if (hide) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.settings_hide_icon_dialog_title)
                                .setMessage(R.string.settings_hide_icon_dialog_text)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                    setAppIconHide(true);
                                })
                                .show();
                    } else {
                        setAppIconHide(false);
                    }
                    return false;
                });
            }

            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.settings_title_about)
                            .setMessage(R.string.settings_about_msg)
                            .setPositiveButton(R.string.confirm, null)
                            .show();
                    return false;
                });
            }

            Preference qqPreference = findPreference("join_qq");
            if (qqPreference != null) {
                qqPreference.setOnPreferenceClickListener(preference -> {
                    if (! joinQQGroup("5jcvi8iIuZuQVqm4HZXlVhxoMQ1RLVtF")) {
                        Snackbar.make(requireView(),
                                R.string.settings_open_qq_fail,
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                    return false;
                });
            }

            Preference howToUsePreference = findPreference("how_to_use");
            if (howToUsePreference != null) {
                howToUsePreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.how_to_use_title)
                        .setMessage(R.string.how_to_use_msg)
                        .setPositiveButton(R.string.confirm, null)
                        .show();
                    return false;
                });
            }

            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                donatePreference.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), DonateActivity.class));
                    return false;
                });
            }

            Preference noMoneyPlanPreference = findPreference("no_money");
            if (noMoneyPlanPreference != null) {
                noMoneyPlanPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.settings_title_no_money_plan)
                            .setMessage(R.string.dialog_text_no_money)
                            .setNegativeButton(R.string.dialog_button_text_no_money_link, (dialogInterface, i) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NoMoneyPlan"));
                                startActivity(intent);
                            })
                            .setPositiveButton(R.string.confirm, null)
                            .show();
                    return false;
                });
            }
        }

        public boolean joinQQGroup(String key) {
            Intent intent = new Intent();
            intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
            // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent);
                return true;
            } catch (Exception e) {
                // 未安装手Q或安装的版本不支持
                return false;
            }
        }

        public void setAppIconHide(boolean hide) {
            PackageManager pm = requireActivity().getPackageManager();
            ComponentName componentName = new ComponentName(requireActivity(), "com.zhizi42.diymiuicard.StartMainActivity");

            int newState = hide ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP);
        }

    }

    @Keep
    public static class CustomHookFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.custom_hook_preferences, rootKey);
        }
    }
}