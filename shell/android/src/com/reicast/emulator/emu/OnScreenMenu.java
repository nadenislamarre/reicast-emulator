package com.reicast.emulator.emu;

import java.io.File;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.reicast.emulator.MainActivity;
import com.reicast.emulator.R;
import com.reicast.emulator.config.ConfigureFragment;
import com.reicast.emulator.periph.VmuLcd;

public class OnScreenMenu {

	private GL2JNIActivity mContext;
	private SharedPreferences prefs;
	private LinearLayout hlay;
	private LayoutParams params;
	private int frameskip;
	private boolean widescreen;
	private boolean limitframes;
	private boolean audiodisabled;

	private VmuLcd vmuLcd;

	private Vector<PopupWindow> popups;

	private File sdcard = Environment.getExternalStorageDirectory();
	private String home_directory = sdcard + "/dc";

	public OnScreenMenu(Context mContext, SharedPreferences prefs) {
		if (mContext instanceof GL2JNIActivity) {
			this.mContext = (GL2JNIActivity) mContext;
		}
		popups = new Vector<PopupWindow>();
		if (prefs != null) {
			this.prefs = prefs;
			home_directory = prefs.getString("home_directory", home_directory);
			widescreen = ConfigureFragment.widescreen;
			frameskip = ConfigureFragment.frameskip;
		}
		vmuLcd = new VmuLcd(mContext);
		vmuLcd.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				OnScreenMenu.this.mContext.toggleVmu();
			}
		});
	}

	void displayDebugPopup(final PopupWindow popUp) {
		mContext.displayDebug(new DebugPopup(mContext));
	}

	public class FpsPopup extends PopupWindow {
		
		private TextView fpsText;

		public FpsPopup(Context c) {
			super(c);
			setBackgroundDrawable(null);
			fpsText = new TextView(mContext);
			fpsText.setTextAppearance(mContext,  
				       R.style.fpsOverlayText);
			fpsText.setGravity(Gravity.CENTER);
			fpsText.setText("XX");
			setContentView(fpsText);
		}

		public void setText(int frames) {
			fpsText.setText(String.valueOf(frames));
			fpsText.invalidate();
		}
	}

	public class DebugPopup extends PopupWindow {

		public DebugPopup(Context c) {
			super(c);
			setBackgroundDrawable(null);
			int p = getPixelsFromDp(60, mContext);
			LayoutParams debugParams = new LayoutParams(p, p);

			LinearLayout hlay = new LinearLayout(mContext);

			hlay.setOrientation(LinearLayout.HORIZONTAL);

			hlay.addView(addbut(R.drawable.up, new OnClickListener() {
				public void onClick(View v) {
					popups.remove(this);
					dismiss();
					mContext.displayPopUp(OnScreenMenu.this.mContext.popUp);
				}
			}), debugParams);

			hlay.addView(addbut(R.drawable.clear_cache, new OnClickListener() {
				public void onClick(View v) {
					JNIdc.send(0, 0); // Killing texture cache
					dismiss();
				}
			}), debugParams);

			hlay.addView(addbut(R.drawable.profiler, new OnClickListener() {
				public void onClick(View v) {
					JNIdc.send(1, 3000); // sample_Start(param);
					dismiss();
				}
			}), debugParams);

			hlay.addView(addbut(R.drawable.profiler, new OnClickListener() {
				public void onClick(View v) {
					JNIdc.send(1, 0); // sample_Start(param);
					dismiss();
				}
			}), debugParams);

			hlay.addView(addbut(R.drawable.print_stats, new OnClickListener() {
				public void onClick(View v) {
					JNIdc.send(0, 2);
					dismiss(); // print_stats=true;
				}
			}), debugParams);

			hlay.addView(addbut(R.drawable.close, new OnClickListener() {
				public void onClick(View v) {
					popups.remove(this);
					dismiss();
				}
			}), debugParams);

			setContentView(hlay);
			popups.add(this);
		}
	}

	void displayConfigPopup(final PopupWindow popUp) {
		mContext.displayConfig(new ConfigPopup(mContext));
	}

	public class ConfigPopup extends PopupWindow {

		private View fullscreen;
		private View framelimit;
		private View audiosetting;
		private View fdown;
		private View fup;
		private int frames = Config.frameskip;
		private boolean screen = Config.widescreen;
		private boolean limit = Config.limitfps;
		private boolean audio;

		public ConfigPopup(Context c) {
			super(c);
			setBackgroundDrawable(null);
			int p = getPixelsFromDp(60, mContext);
			LayoutParams configParams = new LayoutParams(p, p);

			LinearLayout hlay = new LinearLayout(mContext);

			hlay.setOrientation(LinearLayout.HORIZONTAL);

			hlay.addView(addbut(R.drawable.up, new OnClickListener() {
				public void onClick(View v) {
					popups.remove(this);
					dismiss();
					mContext.displayPopUp(OnScreenMenu.this.mContext.popUp);
				}
			}), configParams);

			fullscreen = addbut(R.drawable.widescreen,
					new OnClickListener() {
				public void onClick(View v) {
					if (screen) {
						JNIdc.widescreen(1);
						screen = true;
						((ImageButton) fullscreen).setImageResource(R.drawable.normal_view);
					} else {
						JNIdc.widescreen(0);
						screen = false;
						((ImageButton) fullscreen).setImageResource(R.drawable.widescreen);
					}
					dismiss();
				}
			});
			if (screen) {
				((ImageButton) fullscreen).setImageResource(R.drawable.normal_view);

			}
			hlay.addView(fullscreen, params);

			fdown = addbut(R.drawable.frames_down, new OnClickListener() {
				public void onClick(View v) {
					frameskip++;
					JNIdc.frameskip(frameskip);
					enableState(frames_up, frames_down);
				}
			});
			fup = addbut(R.drawable.frames_up, new OnClickListener() {
				public void onClick(View v) {
					frameskip--;
					JNIdc.frameskip(frameskip);
					enableState(frames_up, frames_down);
				}
			});

			hlay.addView(frames_up, params);
			hlay.addView(frames_down, params);
			enableState(frames_up, frames_down);


			framelimit = addbut(R.drawable.frames_limit_on,
					new OnClickListener() {
				public void onClick(View v) {
					if (limit) {
						JNIdc.limitfps(0);
						limit = false;
						((ImageButton) audiosetting).setImageResource(R.drawable.frames_limit_on);
					} else {
						JNIdc.limitfps(1);
						limit = true;
						((ImageButton) audiosetting).setImageResource(R.drawable.frames_limit_off);
					}
					dismiss();

				}
			});
			if (limit) {
				((ImageButton) audiosetting).setImageResource(R.drawable.frames_limit_off);
			}
			hlay.addView(framelimit, params);

			audiosetting = addbut(R.drawable.enable_sound,
					new OnClickListener() {
				public void onClick(View v) {
					if (audio) {
						((ImageButton) audiosetting).setImageResource(R.drawable.mute_sound);
						mContext.mView.audioDisable(false);
					} else {
						((ImageButton) audiosetting).setImageResource(R.drawable.enable_sound);
						mContext.mView.audioDisable(true);
					}
					dismiss();
					audio = true;
				}
			});
			audio = prefs.getBoolean("sound_enabled", true);
			if (audio) {
				((ImageButton) audiosetting).setImageResource(R.drawable.mute_sound);
			}
			hlay.addView(audiosetting, params);

			hlay.addView(addbut(R.drawable.close, new OnClickListener() {
				public void onClick(View v) {
					popups.remove(this);
					dismiss();
				}
			}), configParams);

			setContentView(hlay);
			popups.add(this);
		}
	}

	private void enableState(View frames_up, View frames_down) {
		if (frameskip <= 0) {
			frames_down.setEnabled(false);
		} else {
			frames_down.setEnabled(true);
		}
		if (frameskip >= 5) {
			frames_up.setEnabled(false);
		} else {
			frames_up.setEnabled(true);
		}
	}

	public boolean dismissPopUps() {
		for (PopupWindow popup : popups) {
			if (popup.isShowing()) {
				popup.dismiss();
				popups.remove(popup);
				return true;
			}
		}
		return false;
	}

	public static int getPixelsFromDp(float dps, Context context) {
		return (int) (dps * context.getResources().getDisplayMetrics().density + 0.5f);
	}

	public VmuLcd getVmu() {
		return vmuLcd;
	}

	View addbut(int x, OnClickListener ocl) {
		ImageButton but = new ImageButton(mContext);

		but.setImageResource(x);
		but.setScaleType(ScaleType.FIT_CENTER);
		but.setOnClickListener(ocl);

		return but;
	}

	public class VmuPopup extends PopupWindow {
		LayoutParams vparams;
		LinearLayout vlay;

		public VmuPopup(Context c) {
			super(c);
			setBackgroundDrawable(null);
			int pX = OnScreenMenu.getPixelsFromDp(80, mContext);
			int pY = OnScreenMenu.getPixelsFromDp(56, mContext);
			vparams = new LayoutParams(pX, pY);
			vlay = new LinearLayout(mContext);
			vlay.setOrientation(LinearLayout.HORIZONTAL);
			setContentView(vlay);
		}

		public void showVmu() {
			vmuLcd.configureScale(80);
			vlay.addView(vmuLcd, vparams);
		}

	}

	public class MainPopup extends PopupWindow {

		private View rsticksetting;

		public MainPopup(Context c) {
			super(c);
			setBackgroundDrawable(null);
			int p = getPixelsFromDp(60, mContext);
			params = new LayoutParams(p, p);
			hlay = new LinearLayout(mContext);
			hlay.setOrientation(LinearLayout.HORIZONTAL);

			int vpX = getPixelsFromDp(60, mContext);
			int vpY = getPixelsFromDp(42, mContext);
			LinearLayout.LayoutParams vmuParams = new LinearLayout.LayoutParams(
					vpX, vpY);
			vmuParams.weight = 1.0f;
			vmuParams.gravity = Gravity.CENTER_VERTICAL;
			vmuParams.rightMargin = 4;
			hlay.addView(vmuLcd, vmuParams);

			hlay.addView(addbut(R.drawable.up, new OnClickListener() {
				public void onClick(View v) {
					popups.remove(MainPopup.this);
					dismiss();
				}
			}), params);

			hlay.addView(addbut(R.drawable.vmu_swap, new OnClickListener() {
				public void onClick(View v) {
					JNIdc.vmuSwap();
					dismiss();
				}
			}), params);

			rsticksetting = addbut(R.drawable.toggle_a_b,
					new OnClickListener() {
				public void onClick(View v) {
					if (prefs.getBoolean("right_buttons", true)) {
						prefs.edit().putBoolean("right_buttons", false).commit();
						((ImageButton) rsticksetting).setImageResource(R.drawable.toggle_a_b);
					} else {
						prefs.edit().putBoolean("right_buttons", true).commit();
						((ImageButton) rsticksetting).setImageResource(R.drawable.toggle_r_l);
					}
					dismiss();
				}
			});
			if (prefs.getBoolean("right_buttons", true)) {
				((ImageButton) rsticksetting).setImageResource(R.drawable.toggle_r_l);
			}
			hlay.addView(rsticksetting, params);

			hlay.addView(addbut(R.drawable.config, new OnClickListener() {
				public void onClick(View v) {
					displayConfigPopup(MainPopup.this);
					popups.remove(MainPopup.this);
					dismiss();
				}
			}), params);

			hlay.addView(addbut(R.drawable.disk_unknown, new OnClickListener() {
				public void onClick(View v) {
					displayDebugPopup(MainPopup.this);
					popups.remove(MainPopup.this);
					dismiss();
				}
			}), params);

			hlay.addView(addbut(R.drawable.close, new OnClickListener() {
				public void onClick(View v) {
					Intent inte = new Intent(mContext, MainActivity.class);
					mContext.startActivity(inte);
					((Activity) mContext).finish();
				}
			}), params);

			setContentView(hlay);
		}

		public void showVmu() {
			vmuLcd.configureScale(60);
			hlay.addView(vmuLcd, 0, params);
		}
	}
}
