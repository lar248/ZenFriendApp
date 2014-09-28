package com.example.zenfriend1;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class MainActivity extends FragmentActivity implements TabListener {
	
	ViewPager viewPager;
	ActionBar actionBar;
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_main);
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				actionBar.setSelectedNavigationItem(arg0);//tab selected should also equal number on a certain page
				Log.d("VIVZ", "onPageSelected at "+" position" +arg0);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				Log.d("VIVZ", "onPageScrolled at "+" position" +arg0+ " from " +arg1+ " with number of pixels="+arg2);
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				if(arg0==ViewPager.SCROLL_STATE_IDLE) {
					Log.d("VIVZ", "onPageScrollStateChanged Idle");
				} 
				if(arg0==ViewPager.SCROLL_STATE_DRAGGING) {
					Log.d("VIVZ", "onPageScrollStateChanged Dragging");
				} 
				if(arg0==ViewPager.SCROLL_STATE_SETTLING) {
					Log.d("VIVZ", "onPageScrollStateChanged Settling");
				}
				
				
			}
		});
		
		actionBar = getActionBar();
		setTitle("ZenFriend");
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		ActionBar.Tab tab1 = actionBar.newTab();
		tab1.setIcon(R.drawable.ic_tab2);
		tab1.setText("  SUPPORT");
		tab1.setTabListener(this);
		
		ActionBar.Tab tab2 = actionBar.newTab();
		tab2.setIcon(R.drawable.calendar);
		tab2.setText("  CALENDAR");
		tab2.setTabListener(this);
		
		actionBar.addTab(tab1);
		actionBar.addTab(tab2);
		
	}
	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}
	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		viewPager.setCurrentItem(arg0.getPosition()); //get current page when I have clicked a particular tab		
	}
	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}

class MyAdapter extends FragmentPagerAdapter {

	public MyAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int arg0) {
		Fragment fragment=null;
		if (arg0==0) {
			fragment = new FragmentA();
		}
		if (arg0==1) {
			fragment = new FragmentB();
		}
		return fragment;
	}

	@Override
	public int getCount() {
		return 2;
	}
	
}