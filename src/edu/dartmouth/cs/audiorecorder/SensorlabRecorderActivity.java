package edu.dartmouth.cs.audiorecorder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.example.zenfriend1.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

@TargetApi(3)
public class SensorlabRecorderActivity extends Activity implements OnClickListener {
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String text = msg.getData().getString(AudioRecorderService.AUDIORECORDER_NEWTEXT_CONTENT);
			mTvGenericText.setText(text);
		}
	};

	static final String tag = "SensorLabRecorder";
	public static HashMap<Double, Integer> colorMap = new HashMap<Double, Integer>();
	
	public TextView currentMonth;
	public Button selectedDayMonthYearButton;
	public ImageView prevMonth;
	public ImageView nextMonth;
	public GridView calendarView;
	public GridCellAdapter adapter;
	public Calendar _calendar;
	public int month;
	public int year;
	public int day;
	private double date =(double)Integer.parseInt(""+day+month+year);
	
	@SuppressWarnings("unused")
	@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi" })
	private final DateFormat dateFormatter = new DateFormat();
	public static final String dateTemplate = "MMMM yyyy";

	private static Handler sMessageHandler;
	
	private AudioRecorderStatusRecevier mAudioRecorderStatusRecevier;
	private ToggleButton mTbManageRecorder;
	private TextView mTvAudioRecorderStatus;
	private TextView mTvGenericText;
	private RehearsalAudioRecorder recorder;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_calendar_view);

		_calendar = Calendar.getInstance(Locale.getDefault());
		month = _calendar.get(Calendar.MONTH) + 1;
		year = _calendar.get(Calendar.YEAR);
		day = _calendar.get(Calendar.DAY_OF_MONTH);
		Log.d(tag, "Calendar Instance:= " + "Month: " + month + " " + "Year: "
				+ year);
		
		currentMonth = (TextView) this.findViewById(R.id.currentMonth);
		currentMonth.setText(DateFormat.format(dateTemplate, _calendar.getTime()));

		calendarView = (GridView) this.findViewById(R.id.calendar);

		// Initialised
		adapter = new GridCellAdapter(getApplicationContext(),
				R.id.calendar_day_gridcell, month, year, day);
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
		
		mAudioRecorderStatusRecevier = new AudioRecorderStatusRecevier();

		mTbManageRecorder = (ToggleButton) findViewById(R.id.tbManageRecorder);
		mTvAudioRecorderStatus = (TextView) findViewById(R.id.tvAudioRecorderStatus);
		mTvGenericText = (TextView) findViewById(R.id.tvGenericText);

		mTbManageRecorder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == AudioRecorderService.isServiceRunning.get()) {
					// the button is already coherent, do nothing
					return;
				}
				Intent intent = new Intent(SensorlabRecorderActivity.this, AudioRecorderService.class);
				if (isChecked) {
					startService(intent);
					System.out.println(recorder.percent);
				} else {
					stopService(intent);
				}
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		registerReceiver(mAudioRecorderStatusRecevier, new IntentFilter(AudioRecorderService.AUDIORECORDER_ON));
		registerReceiver(mAudioRecorderStatusRecevier, new IntentFilter(AudioRecorderService.AUDIORECORDER_OFF));
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(mAudioRecorderStatusRecevier);
	}

	@Override
	public void onDestroy() {
		Log.d(tag, "Destroying View ...");
		super.onDestroy();
	}
	
	public int getHour() {
		return _calendar.get(Calendar.HOUR_OF_DAY);
	}
	
	public int getMinute() {
		return _calendar.get(Calendar.MINUTE);
	}
	
	public int getSecond() {
		return _calendar.get(Calendar.SECOND);
	}
	
	public void setDailyStress() {
		if (getHour()==23 && getMinute()==55 && getSecond()<=5) {
			//colorMap.put();
		}
	}
	
	// Inner Class
	public class GridCellAdapter extends BaseAdapter implements OnClickListener {
		private static final String tag = "GridCellAdapter";
		private final Context _context;
		
		//This will need to be implemented in another class...		
		private final List<String> list;
		private static final int DAY_OFFSET = 1;
		private final String[] weekdays = new String[] { "Sun", "Mon", "Tue",
				"Wed", "Thu", "Fri", "Sat" };
		private final String[] months = { "January", "February", "March",
				"April", "May", "June", "July", "August", "September",
				"October", "November", "December" };
		private final int[] daysOfMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30,
				31, 30, 31 };
		private int daysInMonth;
		private int currentDayOfMonth;
		private int currentWeekDay;
		private Button gridcell;
		private Button background;
		private TextView num_events_per_day;
		private final HashMap<String, Integer> eventsPerMonthMap;
		private final SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"dd-MMM-yyyy");

		// Days in Current Month
		public GridCellAdapter(Context context, int textViewResourceId,
				int month, int year, int day) {
			super();
			this._context = context;
			this.list = new ArrayList<String>();
			Log.d(tag, "==> Passed in Date FOR Month: " + month + " "
					+ "Year: " + year);
			Calendar calendar = Calendar.getInstance();			
			setCurrentDayOfMonth(calendar.get(Calendar.DAY_OF_MONTH));
			setCurrentWeekDay(calendar.get(Calendar.DAY_OF_WEEK));
			Log.d(tag, "New Calendar:= " + calendar.getTime().toString());
			Log.d(tag, "CurrentDayOfWeek: " + getCurrentWeekDay());
			Log.d(tag, "CurrentDayOfMonth: " + getCurrentDayOfMonth());

			// Print Month
			printMonth(month, year);

			// Find Number of Events
			eventsPerMonthMap = findNumberOfEventsPerMonth(year, month, day);
		}

		private String getMonthAsString(int i) {
			return months[i];
		}

		private String getWeekDayAsString(int i) {
			return weekdays[i];
		}

		private int getNumberOfDaysOfMonth(int i) {
			return daysOfMonth[i];
		}

		public String getItem(int position) {
			return list.get(position);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		/**
		 * Prints Month
		 * 
		 * @param mm
		 * @param yy
		 */
		private void printMonth(int mm, int yy) {
			Log.d(tag, "==> printMonth: MM: " + mm + " " + "YY: " + yy);
			int trailingSpaces = 0;
			int daysInPrevMonth = 0;
			int prevMonth = 0;
			int prevYear = 0;
			int nextMonth = 0;
			int nextYear = 0;

			int currentMonth = mm - 1;
			String currentMonthName = getMonthAsString(currentMonth);
			daysInMonth = getNumberOfDaysOfMonth(currentMonth);

			Log.d(tag, "Current Month: " + " " + currentMonthName + " having "
					+ daysInMonth + " days.");

			GregorianCalendar cal = new GregorianCalendar(yy, currentMonth, 1);
			Log.d(tag, "Gregorian Calendar:= " + cal.getTime().toString());

			if (currentMonth == 11) {
				prevMonth = currentMonth - 1;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				nextMonth = 0;
				prevYear = yy;
				nextYear = yy + 1;
				Log.d(tag, "*->PrevYear: " + prevYear + " PrevMonth:"
						+ prevMonth + " NextMonth: " + nextMonth
						+ " NextYear: " + nextYear);
			} else if (currentMonth == 0) {
				prevMonth = 11;
				prevYear = yy - 1;
				nextYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				nextMonth = 1;
				Log.d(tag, "**--> PrevYear: " + prevYear + " PrevMonth:"
						+ prevMonth + " NextMonth: " + nextMonth
						+ " NextYear: " + nextYear);
			} else {
				prevMonth = currentMonth - 1;
				nextMonth = currentMonth + 1;
				nextYear = yy;
				prevYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				Log.d(tag, "***---> PrevYear: " + prevYear + " PrevMonth:"
						+ prevMonth + " NextMonth: " + nextMonth
						+ " NextYear: " + nextYear);
			}

			int currentWeekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
			trailingSpaces = currentWeekDay;

			Log.d(tag, "Week Day:" + currentWeekDay + " is "
					+ getWeekDayAsString(currentWeekDay));
			Log.d(tag, "No. Trailing space to Add: " + trailingSpaces);
			Log.d(tag, "No. of Days in Previous Month: " + daysInPrevMonth);

			if (cal.isLeapYear(cal.get(Calendar.YEAR)))
				if (mm == 2)
					++daysInMonth;
				else if (mm == 3)
					++daysInPrevMonth;

			// Trailing Month days
			for (int i = 0; i < trailingSpaces; i++) {
				Log.d(tag,
						"PREV MONTH:= "
								+ prevMonth
								+ " => "
								+ getMonthAsString(prevMonth)
								+ " "
								+ String.valueOf((daysInPrevMonth
										- trailingSpaces + DAY_OFFSET)
										+ i));
				list.add(String
						.valueOf((daysInPrevMonth - trailingSpaces + DAY_OFFSET)
								+ i)
						+ "-GREY"
						+ "-"
						+ getMonthAsString(prevMonth)
						+ "-"
						+ prevYear);
			}

			// Current Month Days
			for (int i = 1; i <= daysInMonth; i++) {
				Log.d(currentMonthName, String.valueOf(i) + " "
						+ getMonthAsString(currentMonth) + " " + yy);
				if (i == getCurrentDayOfMonth()) {
					list.add(String.valueOf(i) + "-BLUE" + "-"
							+ getMonthAsString(currentMonth) + "-" + yy);
				} else {
					list.add(String.valueOf(i) + "-WHITE" + "-"
							+ getMonthAsString(currentMonth) + "-" + yy);
				}
			}

			// Leading Month days
			for (int i = 0; i < list.size() % 7; i++) {
				Log.d(tag, "NEXT MONTH:= " + getMonthAsString(nextMonth));
				list.add(String.valueOf(i + 1) + "-GREY" + "-"
						+ getMonthAsString(nextMonth) + "-" + nextYear);
			}
		}

		/**
		 * @param year
		 * @param month
		 * @param day
		 * @return
		 */
		private HashMap<String, Integer> findNumberOfEventsPerMonth(int year,
				int month, int day) {
			HashMap<String, Integer> map = new HashMap<String, Integer>();

			return map;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = (LayoutInflater) _context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.screen_gridcell, parent, false);
			}

			// Get a reference to the Day gridcell
			gridcell = (Button) row.findViewById(R.id.calendar_day_gridcell);
			gridcell.setOnClickListener(this);
			background = (Button) row.findViewById(R.id.calendar);

			// ACCOUNT FOR SPACING
			Log.d(tag, "Current Day: " + getCurrentDayOfMonth());
			String[] day_color = list.get(position).split("-");
			String theday = day_color[0];
			String themonth = day_color[2];
			String theyear = day_color[3];
			if ((!eventsPerMonthMap.isEmpty()) && (eventsPerMonthMap != null)) {
				if (eventsPerMonthMap.containsKey(theday)) {
					num_events_per_day = (TextView) row
							.findViewById(R.id.num_events_per_day);
					Integer numEvents = (Integer) eventsPerMonthMap.get(theday);
					num_events_per_day.setText(numEvents.toString());
				}
			}

			// Set the Day GridCell
			gridcell.setText(theday);
			gridcell.setTag(theday + "-" + themonth + "-" + theyear);
			Log.d(tag, "Setting GridCell " + theday + "-" + themonth + "-"
					+ theyear);
			if (day_color[1].equals("GREY")) {
				gridcell.setTextColor(getResources().getColor(R.color.lightgray));
				//gridcell.setBackgroundColor(getResources().getColor(R.color.lightgray));
			}
			if (day_color[1].equals("WHITE")) {
				gridcell.setTextColor(getResources().getColor(R.color.black));
				//gridcell.setBackgroundColor(getResources().getColor(R.color.black));
			}
			if (day_color[1].equals("BLUE")) {
				gridcell.setTextColor(getResources().getColor(R.color.darkorrange));
				//gridcell.setBackgroundColor(getResources().getColor(R.color.darkorrange));
			}
			return row;
		}

		@Override
		public void onClick(View view) {
			String date_month_year = (String) view.getTag();
			//selectedDayMonthYearButton.setText("Selected: " + date_month_year);
			Log.e("Selected date", date_month_year);
			try {
				Date parsedDate = dateFormatter.parse(date_month_year);
				Log.d(tag, "Parsed Date: " + parsedDate.toString());

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		public int getCurrentDayOfMonth() {
			return currentDayOfMonth;
		}

		private void setCurrentDayOfMonth(int currentDayOfMonth) {
			this.currentDayOfMonth = currentDayOfMonth;
		}

		public void setCurrentWeekDay(int currentWeekDay) {
			this.currentWeekDay = currentWeekDay;
		}

		public int getCurrentWeekDay() {
			return currentWeekDay;
		}	
	}
	
	@Override
	public void onPause() {
		sMessageHandler = null;
		super.onPause();
	}

	private void updateStatus() {
		if (AudioRecorderService.isServiceRunning.get()) {
			mTbManageRecorder.setChecked(true);
			//mTvAudioRecorderStatus.setText(String.format(getString(R.string.service_status), "on"));
		} else {
		    mTbManageRecorder.setChecked(false);
			//mTvAudioRecorderStatus.setText(String.format(getString(R.string.service_status), "off"));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateStatus();
		sMessageHandler = mHandler;
	}
	
	public static Handler getHandler() {
		return sMessageHandler;
	}

	class AudioRecorderStatusRecevier extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(AudioRecorderService.AUDIORECORDER_ON)
					|| intent.getAction().equals(AudioRecorderService.AUDIORECORDER_OFF)) {
				updateStatus();
			}
		}
	}
	

	
	/**
	 * 
	 * @param month
	 * @param year
	 */
	private void setGridCellAdapterToDate(int month, int year, int day) {
		adapter = new GridCellAdapter(getApplicationContext(),
				R.id.calendar_day_gridcell, month, year, day);
		_calendar.set(year, month - 1, _calendar.get(Calendar.DAY_OF_MONTH));
		currentMonth.setText(DateFormat.format(dateTemplate,
				_calendar.getTime()));
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
	}
	
	public void onClick(View v) {
		if (v == prevMonth) {
			if (month <= 1) {
				month = 12;
				year--;
			} else {
				month--;
			}
			Log.d(tag, "Setting Prev Month in GridCellAdapter: " + "Month: "
					+ month + " Year: " + year);
			setGridCellAdapterToDate(month, year, day);
		}
		if (v == nextMonth) {
			if (month > 11) {
				month = 1;
				year++;
			} else {
				month++;
			}
			Log.d(tag, "Setting Next Month in GridCellAdapter: " + "Month: "
					+ month + " Year: " + year);
			setGridCellAdapterToDate(month, year, day);
		}
	}
	
//	public class OutgoingReceiver extends BroadcastReceiver {
//		 
//	    public static final String CUSTOM_INTENT = "addison.slabaugh.custom.intent.action.TEST";
//	 
//	    @Override
//	    public void onReceive(Context context, Intent intent) {
//	        System.out.println("HIT OUTGOING");
//	        Intent i = new Intent();
//	        i.setAction(CUSTOM_INTENT);
//	        context.sendBroadcast(i);
//	    }
//	}
//	
//	public class IncomingReceiver extends BroadcastReceiver {
//		 
//	    @Override
//	    public void onReceive(Context context, Intent intent) {
//	        if (intent.getAction().equals(OutgoingReceiver.CUSTOM_INTENT)) {
//	            System.out.println("GOT THE INTENT");
//	        }
//	    }
//	}
	
}