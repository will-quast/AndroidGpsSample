package com.example.gpssample;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	public static final String TAG = MainActivity.class.getName();
	
	private static final long DURATION_TO_FIX_LOST_MS = 10000;
	
	private LocationManager locationManager;
	private MyGpsListener gpsListener;
	
	private TextView latitudeText;
	private TextView longitudeText;
	private TextView qualityText;
	private TextView satsTotal;
	private TextView satsUsed;
	
	private boolean gpsEnabled;
	private boolean gpsFix;
	private double latitude;
	private double longitude;
	private int satellitesTotal;
	private int satellitesUsed;
	private float accuracy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		latitudeText = (TextView) findViewById(R.id.latitudeText);
		longitudeText = (TextView) findViewById(R.id.longitudeText);
		qualityText = (TextView) findViewById(R.id.qualityText);
		satsTotal = (TextView) findViewById(R.id.satsTotalText);
		satsUsed = (TextView) findViewById(R.id.satsUsedText);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		gpsListener = new MyGpsListener();
		locationManager.addGpsStatusListener(gpsListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		if (locationManager != null) {
			locationManager.removeGpsStatusListener(gpsListener);
			locationManager.removeUpdates(gpsListener);
			locationManager = null;
		}
	}
	
	private void updateView() {
		
		latitudeText.setText(Double.toString(latitude));
		longitudeText.setText(Double.toString(longitude));
		qualityText.setText(getGrade());
		satsTotal.setText(Integer.toString(satellitesTotal));
		satsUsed.setText(Integer.toString(satellitesUsed));
	}
	
	private String getGrade() {
		
		if (!gpsEnabled) {
			return "Disabled";
		}
		
		if (!gpsFix) {
			return "Waiting for Fix";
		}
		
		if (accuracy <= 10) {
			return "Good";
		}
		
		if (accuracy <= 30) {
			return "Fair";
		}
		
		if (accuracy <= 100) {
			return "Bad";
		}
		
		return "Unusable";
	}
	
	protected class MyGpsListener implements GpsStatus.Listener, LocationListener {
		
		private long previousLocationSystemTime = 0;
		private List<Float> rollingAverageData = new LinkedList<Float>();
		
		@Override
		public void onGpsStatusChanged(int changeType) {
			
			if (locationManager != null) {
				
				GpsStatus status = locationManager.getGpsStatus(null);
				
				switch(changeType) {
					case GpsStatus.GPS_EVENT_FIRST_FIX:
						gpsEnabled = true;
						gpsFix = true;
						break;
					case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
						gpsEnabled = true;
						gpsFix = System.currentTimeMillis() - previousLocationSystemTime < DURATION_TO_FIX_LOST_MS;
						break;
					case GpsStatus.GPS_EVENT_STARTED:
						gpsEnabled = true;
						gpsFix = false;
						break;
					case GpsStatus.GPS_EVENT_STOPPED:
						gpsEnabled = false;
						gpsFix = false;
						break;
					default:
						Log.w(TAG, "unknown GpsStatus event type. "+changeType);
						return;
				}
				
				int newSatTotal = 0;
				int newSatUsed = 0;
				for(GpsSatellite sat : status.getSatellites()) {
					newSatTotal++;
					if (sat.usedInFix()) {
						newSatUsed++;
					}
				}
				satellitesTotal = newSatTotal;
				satellitesUsed = newSatUsed;
				
				updateView();
			}
		}

		@Override
		public void onLocationChanged(Location location) {
			
			previousLocationSystemTime = System.currentTimeMillis();
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			
			if (location.hasAccuracy()) {
				updateRollingAverage(location.getAccuracy());
			}
			
			updateView();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			/* dont need this info */
		}

		@Override
		public void onProviderEnabled(String provider) {
			/* dont need this info */
		}

		@Override
		public void onProviderDisabled(String provider) {
			/* dont need this info */
		}
		
		private void updateRollingAverage(float value) {
			rollingAverageData.add(value);
			if (rollingAverageData.size() > 10) {
				rollingAverageData.remove(0);
			}
			
			float average = 0.0f;
			for(Float number : rollingAverageData) {
				average += number;
			}
			average = average / rollingAverageData.size();
			
			accuracy = average;
		}
	}
}
