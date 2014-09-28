package com.example.zenfriend1;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Build;

public class MyListDialog extends ActionBarActivity {
	
	ListView l;
	String[] eat ={"Sip Green Tea", "Nosh On Chocolate", "Bite Into A Mango", "Chew Gum", "Slurp Some Honey", "Munch A Crunchy Snack"};
	String[] peace ={"Meditate", "Lay Your Head On A Pillow", "Think Of Something Happy", "Remember To Breathe", "Try Progressive Relaxation", "Count Backward", "Close Your Eyes"};
	String[] relax = {"Give Yourself A Hand Massage", "Try Acupressure", "Rub Your Feet Over A Golf Ball", "Squeeze A Stress Ball", "Drip Cold Water On Your Wrists", "Brush Your Hair"};
	String[] environments ={"Be Alone", "Create A Zen Zone", "Find The Sun", "Look Out The Window", "Get Organized"};
	String[] exercise ={"Yoga", "Stretch", "Run In Place", "Take A Quick Walk", "Do A Pump Set", "Dance"};
	String[] social={"Cuddle With A Pet", "Laugh", "Talk To A Friend", "Start Planning A Vacation", "Do A Crossword Puzzle", "Play A Game", "Watch A Movie Or TV Show"};
	String[] creativity ={"Write It Down", "Draw Everywhere", "Build Something", "Listen To Your Favorite Song", "Record Video/Sound"};
	String[] smell ={"Smell Some Flowers", "Try Aromatherapy", "Sniff Citrus", "Nose Full of Coffee"};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_list_dialog);
		Intent intent = getIntent();
		ImageView myImage;
		ArrayAdapter<String> adapter=null;
		if (intent != null) {
			int imageId = intent.getIntExtra("imageId", R.drawable.exercise);
			String supportName = intent.getStringExtra("supportName");
			l=(ListView) findViewById(R.id.listView1);
			if (supportName.equals("eat")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,eat); //second aparamter is useful for custom row options
			} else if (supportName.equals("peace")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,peace); //second aparamter is useful for custom row options
			} else if (supportName.equals("relax")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,relax); //second aparamter is useful for custom row options
			} else if (supportName.equals("environments")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,environments); //second aparamter is useful for custom row options
			} else if (supportName.equals("exercise")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,exercise); //second aparamter is useful for custom row options
			} else if (supportName.equals("social")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,social); //second aparamter is useful for custom row options
			} else if (supportName.equals("creativity")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,creativity); //second aparamter is useful for custom row options
			} else if (supportName.equals("smell")) {
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,smell); //second aparamter is useful for custom row options
			}
			myImage = (ImageView) findViewById(R.id.imageViewDialog);
			myImage.setImageResource(imageId);
			l.setAdapter(adapter);
		} else {
			Log.d("errorYO", "intent.getExtras() is fucked");
		}

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	public void closeDialog(View v) {
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my_list_dialog, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_my_list_dialog,
					container, false);
			return rootView;
		}
	}
}
