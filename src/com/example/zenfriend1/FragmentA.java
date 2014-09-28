package com.example.zenfriend1;

import java.util.ArrayList;

import com.example.zenfriend1.GridItems;
import com.example.zenfriend1.GridItems.ViewHolder;
import com.example.zenfriend1.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * 
 */
public class FragmentA extends Fragment implements OnItemClickListener{
	GridView myGrid;
	
	public FragmentA() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_a, container, false);
		System.out.println("fragment A worked");
		Log.d("Frag_A", "works");
		super.onCreate(savedInstanceState);
		myGrid = (GridView) view.findViewById(R.id.gridView1);
		myGrid.setAdapter(new GridItems(getActivity()));
		myGrid.setOnItemClickListener(this);
		return view;
	} 

	//Launch dialog box
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		Intent intent = new Intent(getActivity(), MyListDialog.class);
		ViewHolder holder = (ViewHolder) view.getTag();
		SupportItem temp = (SupportItem) holder.mySupport.getTag();
//		Log.d("errorFIRST", "this is frustratin");
		intent.putExtra("imageId", temp.imageId);
		intent.putExtra("supportName", temp.supportName);
		Log.d("name",temp.supportName);
		Log.d("image",temp.imageId+"");
		startActivity(intent);
	}
}

class SupportItem {
	int imageId;
	String supportName;
	SupportItem(int imageId, String supportName) {
		this.imageId = imageId;
		this.supportName = supportName;
	}
}

class GridItems extends BaseAdapter {
	ArrayList<SupportItem> list;
	Context context;
	GridItems(Context context) {
		this.context = context;
		list = new ArrayList<SupportItem>();
		Resources res = context.getResources();
		String[] tempSupportNames = res.getStringArray(R.array.support_names);
		int[] supportImages = {R.drawable.eatanddrink, R.drawable.relaxation1, R.drawable.exercise, R.drawable.peace, R.drawable.socialandfun, R.drawable.environments, R.drawable.creativity, R.drawable.smell};
		for(int i=0; i<8; i++) {
			SupportItem tempSupport = new SupportItem(supportImages[i], tempSupportNames[i]);
			list.add(tempSupport);
			Log.d("what is inside", supportImages.toString());
		}
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int i) {
		return list.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}
	
	class ViewHolder {
		ImageView mySupport;
		ViewHolder(View v) {
			mySupport = (ImageView) v.findViewById(R.id.imageViewSingle);
		}
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		View row = view;
		ViewHolder holder = null;
		//if you are not Recycling
		if (row==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			//inflate take XML object and gives me back Java object
			row = inflater.inflate(R.layout.single_item, viewGroup, false);
			holder = new ViewHolder(row);
			row.setTag(holder);
		} else { //if you are recycling
			holder = (ViewHolder) row.getTag();
		}
		SupportItem tmp = list.get(i);
		holder.mySupport.setImageResource(tmp.imageId);
		holder.mySupport.setTag(tmp);
		return row;
	}
	
}
