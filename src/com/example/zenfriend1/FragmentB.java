package com.example.zenfriend1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import edu.dartmouth.cs.audiorecorder.AudioRecorderService;
import com.example.zenfriend1.R;
import edu.dartmouth.cs.audiorecorder.RehearsalAudioRecorder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * 
 */
public class FragmentB extends Fragment implements OnClickListener {

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String text = msg.getData().getString(
					AudioRecorderService.AUDIORECORDER_NEWTEXT_CONTENT);
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
	private double date = (double) Integer.parseInt("" + day + month + year);

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

	public FragmentB() {
		// Required empty public constructor

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.my_calendar_view, container,
				false);

		try {
			String destPath = "/data/data/" + getClass().getPackage().getName()
					+ "/databases/ZenFriend_DB";
			File f = new File(destPath);
			if (!f.exists()) {
				CopyDB(getActivity().getBaseContext().getAssets().open("mydb"),
						new FileOutputStream(destPath));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		DBAdapter db = new DBAdapter(getActivity());

		// /Add an assignment///
//		db.open();
//		long id = db.insertRecord("45", "5/1/2014");
//		id = db.insertRecord("23", "5/2/2014");
//		id = db.insertRecord("88", "5/3/2014");
//		id = db.insertRecord("100", "5/4/2014");
//		id = db.insertRecord("1", "5/5/2014");
//		db.close();
		
		_calendar = Calendar.getInstance(Locale.getDefault());
		month = _calendar.get(Calendar.MONTH) + 1;
		year = _calendar.get(Calendar.YEAR);
		day = _calendar.get(Calendar.DAY_OF_MONTH);
		Log.d(tag, "Calendar Instance:= " + "Month: " + month + " " + "Year: "
				+ year);
		
		currentMonth = (TextView) view.findViewById(R.id.currentMonth);
		currentMonth.setText(DateFormat.format(dateTemplate, _calendar.getTime()));

		calendarView = (GridView) view.findViewById(R.id.calendar);

		// Initialised
		adapter = new GridCellAdapter(getActivity().getApplicationContext(),
				R.id.calendar_day_gridcell, month, year, day);
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);

		mAudioRecorderStatusRecevier = new AudioRecorderStatusRecevier();

		mTbManageRecorder = (ToggleButton) view
				.findViewById(R.id.tbManageRecorder);
		mTvAudioRecorderStatus = (TextView) view
				.findViewById(R.id.tvAudioRecorderStatus);
		mTvGenericText = (TextView) view.findViewById(R.id.tvGenericText);

		mTbManageRecorder
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked == AudioRecorderService.isServiceRunning
								.get()) {
							// the button is already coherent, do nothing
							return;
						}
						Intent intent = new Intent(getActivity(),
								AudioRecorderService.class);
						if (isChecked) {
							getActivity().startService(intent);
						} else {
							getActivity().stopService(intent);
						}
					}
				});
		return view;
	}

	public void CopyDB(InputStream inputStream, OutputStream outputStream)
			throws IOException {
		// ---copy 1K bytes at a time---
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}
		inputStream.close();
		outputStream.close();
	}

	public void DisplayRecord(Cursor c) {
		Toast.makeText(
				getActivity(),
				"ID: " + c.getString(0) + "\n" + "Calendar Value: "
						+ c.getString(1) + "\n" + "Date: " + c.getString(2),
				Toast.LENGTH_SHORT).show();
	}

	// Inner Class
	public class GridCellAdapter extends BaseAdapter implements OnClickListener {
		private static final String tag = "GridCellAdapter";
		private final Context _context;

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
		DBAdapter datab = new DBAdapter(getActivity());

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

				if (i < getCurrentDayOfMonth()) {
					String date = (currentMonth + 1) + "/" + i + "/" + yy;
					Log.d(tag, date);
					datab.open();
					try {
						Cursor c = datab.getRecord(i);

						double stress_level = Double
								.parseDouble(c.getString(1));
						datab.close();
						Log.d(currentMonthName, String.valueOf(i) + " "
								+ getMonthAsString(currentMonth) + " " + yy);

						if (stress_level > 0 && stress_level <= 10) {
							list.add(String.valueOf(i) + "-a" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 10 && stress_level <= 20) {
							list.add(String.valueOf(i) + "-b" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 20 && stress_level <= 30) {
							list.add(String.valueOf(i) + "-c" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 30 && stress_level <= 40) {
							list.add(String.valueOf(i) + "-d" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 40 && stress_level <= 50) {
							list.add(String.valueOf(i) + "-e" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 50 && stress_level <= 60) {
							list.add(String.valueOf(i) + "-f" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 60 && stress_level <= 70) {
							list.add(String.valueOf(i) + "-g" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 70 && stress_level <= 80) {
							list.add(String.valueOf(i) + "-h" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 80 && stress_level <= 90) {
							list.add(String.valueOf(i) + "-i" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						} else if (stress_level > 90 && stress_level <= 100) {
							list.add(String.valueOf(i) + "-j" + "-"
									+ getMonthAsString(currentMonth) + "-" + yy);
						}
					} catch (CursorIndexOutOfBoundsException e) {
						list.add(String.valueOf(i) + "-WHITE" + "-"
								+ getMonthAsString(currentMonth) + "-" + yy);
					}
				} else if (i == getCurrentDayOfMonth()) {
					list.add(String.valueOf(i) + "-k" + "-"
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

			// Set the Day GridCell//
			gridcell.setText(theday);
			gridcell.setTag(theday + "-" + themonth + "-" + theyear);
			Log.d(tag, "Setting GridCell " + theday + "-" + themonth + "-"
					+ theyear + " with day color " + day_color[1]);
			if (day_color[1].equals("a")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.a));
			}
			if (day_color[1].equals("b")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.b));
			}
			if (day_color[1].equals("c")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.c));
			}
			if (day_color[1].equals("d")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.d));
			}
			if (day_color[1].equals("e")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.e));
			}
			if (day_color[1].equals("f")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.f));
			}
			if (day_color[1].equals("g")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.g));
			}
			if (day_color[1].equals("h")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.h));
			}
			if (day_color[1].equals("i")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.i));
			}
			if (day_color[1].equals("j")) {
				gridcell.setBackgroundColor(getResources().getColor(R.color.j));
			}
			if (day_color[1].equals("k")) {
				gridcell.setBackgroundColor(getResources().getColor(
						R.color.calendar_scroll));
			}
			if (day_color[1].equals("WHITE")) {
				gridcell.setTextColor(getResources().getColor(R.color.gray));
			}
			return row;
		}

		@Override
		public void onClick(View view) {
			String date_month_year = (String) view.getTag();
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
		} else {
			mTbManageRecorder.setChecked(false);
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
			if (intent.getAction()
					.equals(AudioRecorderService.AUDIORECORDER_ON)
					|| intent.getAction().equals(
							AudioRecorderService.AUDIORECORDER_OFF)) {
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
		adapter = new GridCellAdapter(getActivity().getApplicationContext(),
				R.id.calendar_day_gridcell, month, year, day);
		_calendar.set(year, month - 1, _calendar.get(Calendar.DAY_OF_MONTH));
		currentMonth.setText(DateFormat.format(dateTemplate,
				_calendar.getTime()));
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

//	public void onClick(View v) {
//		if (v == prevMonth) {
//			if (month <= 1) {
//				month = 12;
//				year--;
//			} else {
//				month--;
//			}
//			Log.d(tag, "Setting Prev Month in GridCellAdapter: " + "Month: "
//					+ month + " Year: " + year);
//			setGridCellAdapterToDate(month, year, day);
//		}
//		if (v == nextMonth) {
//			if (month > 11) {
//				month = 1;
//				year++;
//			} else {
//				month++;
//			}
//			Log.d(tag, "Setting Next Month in GridCellAdapter: " + "Month: "
//					+ month + " Year: " + year);
//			setGridCellAdapterToDate(month, year, day);
//		}
//	}
}
