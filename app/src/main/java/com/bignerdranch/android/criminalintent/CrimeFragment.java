package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.INotificationSideChannel;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Mason on 10/7/2017.
 */

public class CrimeFragment extends android.support.v4.app.Fragment {

    public static final String ARG_CRIME_ID = "com.jensjensen.android.criminalintent.crime_id";
    public static final String DIALOG_DATE = "DialogDate";
    public static final String DIALOG_TIME = "DialogTime";
    public static String mSuspectId;

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final int REQUEST_CONTACT_PERMISSIONS = 4;
    private static final String[] CONTACTS_PERMISSIONS = {""};

    //member variable for the Crime instance
    private Crime mCrime;
    //allows title text to be edited
    private EditText mTitleField;
    //date button
    private Button mDateButton;
    //time button
    private Button mTimeButton;
    //delete button
    private Button mDeleteButton;
    //checkbox
    private CheckBox mSolvedCheckBox;
    //checkbox
    private CheckBox mPoliceRequiredCheckBox;

    private File mPhotoFile;

    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;

    private Callbacks mCallbacks;

    /*
    required interface for hosting activities
     */
    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
    }

    //implementation Fragment.onCreate(Bundle)
    //notice it is public while Activity.onCreate is protected
    //fragments also have a bundle to which it saves and receives its state
    //retrieves the extra from the activity
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);

        setHasOptionsMenu(true);
    }

    @Override
    public void onPause(){
        super.onPause();

        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    //accepts an UUID, creates an arguments bundle, creates a fragment instance, attaches args to it.
    public static CrimeFragment newInstance(UUID crimeId){
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mCallbacks = null;
    }

    //inflates layout
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //(layout XML, view's parent, add inflated view to parent or not)
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        //hooking up EditText widget
            //note to self: (EditText) is casting
        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //nothing happens
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable s) {
                //nothing happens
            }
        });

        //Hooking up Button widgets. setEnabled makes sure the user can't change anything by touching it, changes appearance to disabled
        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            //when the date button is clicked, creates an instance of the DatePickerFragment.
            //Sets CrimeFragment as Target Fragment of DatePickerFragment, meaning the OS will keep connection open
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        updateTime();
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            //when the date button is clicked, creates an instance of the DatePickerFragment.
            //Sets CrimeFragment as Target Fragment of DatePickerFragment, meaning the OS will keep connection open
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                dialog.show(manager, DIALOG_TIME);
            }
        });

        //hooking up checkbox
        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
                updateCrime();
            }
        });

        //attempting to set up check box, requires police
        mPoliceRequiredCheckBox = (CheckBox) v.findViewById(R.id.police_required);
        mPoliceRequiredCheckBox.setChecked(mCrime.isRequiresPolice());
        mPoliceRequiredCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));
                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null){
            mSuspectButton.setText(mCrime.getSuspect());
        }

        final Intent callContact = new Intent(Intent.ACTION_DIAL);
        mCallSuspectButton = (Button) v.findViewById(R.id.crime_suspect_call);
        //if(mCrime.getSuspect() == null){
            //mCallSuspectButton.setEnabled(false);
        //}
        mCallSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + mCrime.getPhone());
                callContact.setData(number);
                startActivity(callContact);
            }
        });

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null){
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = FileProvider.getUriForFile(getActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        mPhotoFile);
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                List<ResolveInfo> cameraActivities = getActivity()
                        .getPackageManager().queryIntentActivities(captureImage,
                                PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo activity : cameraActivities){
                    getActivity().grantUriPermission(activity.activityInfo.packageName,
                            uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        updatePhotoView();;

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                //return to previous screen
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode != Activity.RESULT_OK){
            return;
        }
        if (requestCode == REQUEST_DATE){
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateCrime();
            updateDate();
        }else if (requestCode == REQUEST_TIME){
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
            mCrime.setDate(date);
            updateCrime();
            updateTime();
        }else if (requestCode == REQUEST_CONTACT && data != null){
            //gets suspect name
            String suspectName = getSuspectName(data);
            mCrime.setSuspect(suspectName);
            updateCrime();
            mSuspectButton.setText(suspectName);

            //gets suspect phone #
            if(hasContactPermission()){
                updateSuspectPhone();
            }else{
                requestPermissions(CONTACTS_PERMISSIONS, REQUEST_CONTACT_PERMISSIONS);
            }
        }else if (requestCode == REQUEST_PHOTO){
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updateCrime();
            updatePhotoView();
        }
    }

    private void updateCrime(){
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        mCallbacks.onCrimeUpdated(mCrime);
    }

    private String getSuspectName(Intent data) {
        Uri contactUri = data.getData();
        //specify which fields you want query to return values for
        String[] queryFields = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };
        //perform query - contactUri is llike a where clause here
        Cursor c = getActivity().getContentResolver()
                .query(contactUri, queryFields, null, null, null);

        try{
            //check if results came back
            if (c.getCount() == 0){
                return null;
            }
            //pull out first column of first row of data: suspects name
            c.moveToFirst();

            mSuspectId = c.getString(0);
            String suspectName = c.getString(1);
            return suspectName;

        } finally {
            c.close();
        }
    }

    private String getSuspectPhoneNumber(String contactId){
        String suspectPhoneNumber = null;

        Uri phoneContactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String[] queryFields = new String[] {
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        };

        //selection criteria, need to ask for a specific contact
        String mSelectionClause = ContactsContract.Data.CONTACT_ID + " = ?";

        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = contactId;

        //query
        Cursor c = getActivity().getContentResolver()
                .query(phoneContactUri, queryFields, mSelectionClause, mSelectionArgs, null);

        try{
            //check if results came back
            if (c.getCount() == 0){
                return null;
            }
            //pull out first column of first row of data: suspects name
            while(c.moveToNext()){
                int phoneType = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if(phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE){
                    suspectPhoneNumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                    break;
                }
            }

        } finally {
            c.close();
        }

        return suspectPhoneNumber;
    }

    private void updateSuspectPhone(){
        String suspectPhoneNumber = getSuspectPhoneNumber(mSuspectId);
        mCrime.setPhone(suspectPhoneNumber);
    }

    private boolean hasContactPermission(){
        int result = ContextCompat.checkSelfPermission(getActivity(), CONTACTS_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case REQUEST_CONTACT_PERMISSIONS:
                if (hasContactPermission()){
                    updateSuspectPhone();
                }
        }
    }

    public void updateDate() {
        Date date = mCrime.getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy");
        mDateButton.setText(sdf.format(date).toString());
    }

    public void updateTime() {
        Date date = mCrime.getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:MM");
        mTimeButton.setText(sdf.format(date).toString());
    }

    private String getCrimeReport(){
        String solvedString = null;
        if(mCrime.isSolved()){
            solvedString = getString(R.string.crime_report_solved);
        }else{
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null){
            suspect = getString(R.string.crime_report_no_suspect);
        }else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }

    private void updatePhotoView(){
        if(mPhotoFile == null || !mPhotoFile.exists()){
            mPhotoView.setImageDrawable(null);
            mPhotoView.setContentDescription(getString(R.string.crime_photo_no_image_description));
        }else{
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(),
                    getActivity());
            mPhotoView.setImageBitmap(bitmap);
            mPhotoView.setContentDescription(getString(R.string.crime_photo_image_description));
        }
    }
}
