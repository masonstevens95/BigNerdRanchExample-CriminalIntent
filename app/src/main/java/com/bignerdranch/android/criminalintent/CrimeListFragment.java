package com.bignerdranch.android.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import javax.security.auth.callback.Callback;

/**
 * Created by Mason on 10/19/2017.
 */

public class CrimeListFragment extends Fragment{

    private RecyclerView mCrimeRecyclerView;
    private ConstraintLayout mEmptyView;
    private Button mNewCrimeButton;
    private CrimeAdapter mAdapter;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    /*
     *Required interface for hosting activities
     */
    public interface Callbacks{
        void onCrimeSelected(Crime crime);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        mCrimeRecyclerView = (RecyclerView) view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (savedInstanceState != null){
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        mEmptyView = (ConstraintLayout) view.findViewById(R.id.empty_list_layout);

        mNewCrimeButton = (Button) view.findViewById(R.id.add_crime_button);
        mNewCrimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCrime();
            }
        });

        updateUI();

        return view;
    }

    //overrides onResume so the list is updated after the Crime Fragment is updated
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        //triggers recreation of action items when user presses show subtitle
        MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);
        if (mSubtitleVisible){
            subtitleItem.setTitle(R.string.hide_subtitle);
        }else{
            subtitleItem.setTitle(R.string.show_subtitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.new_crime:
                addCrime();
                return true;
            case R.id.show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                getActivity().invalidateOptionsMenu();
                updateSubtitle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addCrime() {
        Crime crime = new Crime();
        CrimeLab.get(getActivity()).addCrime(crime);
        updateUI();
        mCallbacks.onCrimeSelected(crime);
    }

    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        int crimeCount = crimeLab.getCrimes().size();
        //String subtitle = getString(R.string.subtitle_format, crimeCount);
        String subtitle = getResources().getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount);

        if (!mSubtitleVisible){
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    public void updateUI(){
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if(mAdapter == null){
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else{
            mAdapter.setCrimes(crimes);
            mAdapter.notifyDataSetChanged();
        }

        mEmptyView.setVisibility(View.VISIBLE);
        if (crimes.size() > 0 ) {
            mEmptyView.setVisibility(View.INVISIBLE);
        }

        updateSubtitle();
    }

    //Creates ViewHolder. This is where binding occurs
    //also handles presses
    //The ABSTRACT CrimeHolder
    private abstract class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView mTitleTextView;
        private TextView mDateTextView;
        private ImageView mSolvedImageView;
        Crime mCrime;

        //constructor, listens to presses
        public CrimeHolder(LayoutInflater inflater, ViewGroup parent, int layout){
            super(inflater.inflate(R.layout.list_item_crime,parent,false));
            itemView.setOnClickListener(this);

            //widgets we are interested in using
            mTitleTextView = (TextView) itemView.findViewById(R.id.crime_title);
            mDateTextView = (TextView) itemView.findViewById(R.id.crime_date);
            mSolvedImageView = (ImageView) itemView.findViewById(R.id.crime_solved);
        }

        //binds data to widget
        public void bind(Crime crime){
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());
            mDateTextView.setText(DateFormat.format("EEEE, MMM dd, yyyy", mCrime.getDate()));
            //handcuffs will be visible if crime is solved. if not, does not take up space for layout, hence gone.
            mSolvedImageView.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
        }

        //listens to presses, connects the list fragment to the actual activity by starting an instance of CrimePagerActivity
        @Override
        public void onClick(View view){
            mCallbacks.onCrimeSelected(mCrime);
        }
    }

    //The REGULAR CrimeHolder
    private class RegularCrimeHolder extends CrimeHolder{

        private Button contactPoliceButton;

        public RegularCrimeHolder(LayoutInflater inflater, ViewGroup parent){
            super(inflater, parent, R.layout.list_item_crime);
        }
    }

    //The SERIOUS CrimeHolder
    private class SeriousCrimeHolder extends CrimeHolder{

        private Button mContactPoliceButton;

        public SeriousCrimeHolder(LayoutInflater inflater, ViewGroup parent){
            super(inflater, parent, R.layout.list_item_crime_police);
        }

        @Override
        public void bind(Crime crime){
            super.bind(crime);

            mContactPoliceButton = (Button) itemView.findViewById(R.id.contact_police_button);
            mContactPoliceButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    Toast.makeText(getActivity(), "Police Contacted for " + mCrime.getTitle(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //Creates Adapter
    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder>{
        private List<Crime> mCrimes;

        //using mPoliceRequired to determine which view is needed
        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }


        public CrimeAdapter(List<Crime> crimes){
            mCrimes = crimes;
        }

        @Override
        public CrimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            //selects view type
            if(viewType == 1){
                return new SeriousCrimeHolder(layoutInflater, parent);
            }
            return new RegularCrimeHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(CrimeHolder holder, int position) {

            //calls bind when RecyclerView requests a CrimeHolder to be bound to a particular name
            Crime crime = mCrimes.get(position);
            holder.bind(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }

        public void setCrimes(List<Crime> crimes){
            mCrimes = crimes;
        }
    }

}
