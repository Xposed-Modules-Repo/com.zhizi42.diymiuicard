package com.zhizi42.diymiuicard;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.google.android.material.snackbar.Snackbar;
import com.zhizi42.diymiuicard.databinding.SettingsActivityBinding;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    private SettingsActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            Insets statusBars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
            binding.toolbar.setMinimumHeight(statusBars.top + toolbarHeight);
            v.setPadding(v.getPaddingLeft(), statusBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                Objects.requireNonNull(pref.getFragment()));
        fragment.setArguments(args);
        //fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    private static long lastClickTime = 0;
    private static int clickCount = 0;

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
                                .setPositiveButton(R.string.confirm, (dialogInterface, i) -> setAppIconHide(true))
                                .show();
                    } else {
                        setAppIconHide(false);
                    }
                    return true;
                });
            }

            LongClickPreference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.settings_title_about)
                            .setMessage(getString(R.string.settings_about_msg, BuildConfig.VERSION_NAME))
                            .setPositiveButton(R.string.confirm, null)
                            .show();
                    return true;
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
                    return true;
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
                    return true;
                });
            }

            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                donatePreference.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), DonateActivity.class));
                    return true;
                });
            }

            Preference noMoneyPlanPreference = findPreference("no_money");
            if (noMoneyPlanPreference != null) {
                noMoneyPlanPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.settings_title_no_money_plan)
                            .setMessage(R.string.settings_dialog_text_no_money)
                            .setNegativeButton(R.string.settings_dialog_button_text_no_money_link, (dialogInterface, i) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NoMoneyPlan"));
                                startActivity(intent);
                            })
                            .setPositiveButton(R.string.confirm, null)
                            .show();
                    return true;
                });
            }

            Preference deleteImageCachePreference = findPreference("delete_image_cache");
            if (deleteImageCachePreference != null) {
                deleteImageCachePreference.setOnPreferenceClickListener(preference -> {
                    Utils.clearCache(requireActivity());
                    return true;
                });
            }

            Preference blackListPreference = findPreference("black_list");
            if (blackListPreference != null) {
                blackListPreference.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), BlackListActivity.class));
                    return true;
                });
            }

            //如果开启了开发者选项就显示所有被隐藏项，反之亦然
            SharedPreferences preferencesPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            Preference showAllCardsPreference = findPreference("show_all_cards");
            Preference debugPreference = findPreference("debug");
            Preference customHookPreference = findPreference("custom_hook");
            Preference devOptGroupPreference = findPreference("dev_opt");
            if (aboutPreference != null &&
                    showAllCardsPreference != null &&
                    debugPreference != null &&
                    customHookPreference != null &&
                    devOptGroupPreference != null) {
                boolean isOpen = preferencesPreferences
                        .getBoolean("dev_mode_open", false);
                devOptGroupPreference.setVisible(isOpen);
                showAllCardsPreference.setVisible(isOpen);
                debugPreference.setVisible(isOpen);
                customHookPreference.setVisible(isOpen);

                aboutPreference.setLongClickListener(preference -> {
                    //连续长按4次就开启开发者选项，显示所有被隐藏项
                    long nowClickTime = System.currentTimeMillis();
                    //如果两次点击时间间隔大于3000ms就重置计数器
                    if (nowClickTime - lastClickTime > 3000) {
                        clickCount = 0;
                    }
                    lastClickTime = nowClickTime;
                    clickCount++;
                    if (clickCount == 3) {
                        Toast.makeText(requireContext(),
                                "long click one times again to open/close dev mode",
                                Toast.LENGTH_LONG).show();
                    } else if (clickCount >= 4) {
                        clickCount = 0;
                        boolean isOpenNow = ! preferencesPreferences
                                .getBoolean("dev_mode_open", false);
                        preferencesPreferences.edit()
                                .putBoolean("dev_mode_open", isOpenNow).apply();
                        devOptGroupPreference.setVisible(isOpenNow);
                        showAllCardsPreference.setVisible(isOpenNow);
                        debugPreference.setVisible(isOpenNow);
                        customHookPreference.setVisible(isOpenNow);
                    }
                    return true;
                });
            }

            //监听设置项的变化同步到remoteprefs
            if (MyXposedService.getService() == null) {
                if (showAllCardsPreference != null) {
                    showAllCardsPreference.setEnabled(false);
                }
                if (debugPreference != null) {
                    debugPreference.setEnabled(false);
                }
            } else {
                SharedPreferences remotePrefs = MyXposedService.getService().getRemotePreferences("settings");
                if (showAllCardsPreference != null) {
                    showAllCardsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        remotePrefs.edit().putBoolean("show_all_cards", (Boolean) newValue).apply();
                        return true;
                    });
                }
                if (debugPreference != null) {
                    debugPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        remotePrefs.edit().putBoolean("debug", (Boolean) newValue).apply();
                        return true;
                    });
                }
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

            Preference classPreference = findPreference("class");
            Preference methodPreference = findPreference("method");
            //监听设置项的变化同步到remoteprefs
            if (MyXposedService.getService() == null) {
                if (classPreference != null) {
                    classPreference.setEnabled(false);
                }
                if (methodPreference != null) {
                    methodPreference.setEnabled(false);
                }
            } else {
                SharedPreferences remotePrefs = MyXposedService.getService().getRemotePreferences("settings");
                if (classPreference != null) {
                    classPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        remotePrefs.edit().putString("class", (String) newValue).apply();
                        return true;
                    });
                }
                if (methodPreference != null) {
                    methodPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        remotePrefs.edit().putString("method", (String) newValue).apply();
                        return true;
                    });
                }
            }
        }
    }
}