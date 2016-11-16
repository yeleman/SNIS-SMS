package com.yeleman.snisrdcsms;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class DataViewerActivity extends CheckedFormActivity {

    private static final String TAG = Constants.getLogTag("DataViewerActivity");
    private Section section = null;
    private LayoutInflater inflater;
    private TableLayout tableLayout;
    private OnSwipeTouchListener onSwipeTouchListener;
    private TextView sectionLabelTextView;
    private Button previousButton;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableSMSReceiving();
        setContentView(R.layout.data_viewer);
        setTitle(getString(R.string.title_activity_data_viewer));

        if (section == null) {
            section = Section.first(Section.class);
        }
        setupOrRefreshUI();
    }


    private boolean hasNextSection() {
        return (section.getId() < Section.count(Section.class));
    }

    private boolean hasPreviousSection() {
        return (section.getId() > 1);
    }

    private Section getPreviousSection() {
        if (hasPreviousSection()) {
            return Section.findById(Section.class, section.getId() - 1);
        }
        return null;
    }

    private Section getNextSection() {
        if (hasNextSection()) {
            return Section.findById(Section.class, section.getId() + 1);
        }
        return null;
    }

    private String getPreviousSectionLabel() {
        if (hasPreviousSection()) {
            return getPreviousSection().getCategoryLabel();
        }
        return "-";
    }

    private String getNextSectionLabel() {
        if (hasNextSection()) {
            return getNextSection().getCategoryLabel();
        }
        return "-";
    }

    private Boolean previousSection() {
        if (hasPreviousSection()) {
            section = getPreviousSection();
            setupOrRefreshUI();
            return true;
        }
        return false;
    }

    private Boolean nextSection() {
        if (hasNextSection()) {
            section = getNextSection();
            setupOrRefreshUI();
            return true;
        }
        return false;
    }

    protected void setupUI() {
        // LinearLayout mainView = (LinearLayout) findViewById(R.id.data_viewer);
        sectionLabelTextView = (TextView) findViewById(R.id.tv_section_label);
        previousButton = (Button) findViewById(R.id.btn_previous);
        nextButton = (Button) findViewById(R.id.btn_next);

        inflater = this.getLayoutInflater();
        tableLayout = (TableLayout) findViewById(R.id.tl_items);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextSection();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousSection();
            }
        });

        onSwipeTouchListener = new OnSwipeTouchListener(DataViewerActivity.this) {
            @Override
            public void onSwipeLeft() {
                Log.d(TAG, "swipe left");
                nextSection();
            }
            @Override
            public void onSwipeRight() {
                Log.d(TAG, "swipe right");
                previousSection();
            }
        };
        tableLayout.setOnTouchListener(onSwipeTouchListener);

        refreshUI();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        onSwipeTouchListener.getGestureDetector().onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    protected void refreshUI() {
        Log.d(TAG, section.getLabel());

        sectionLabelTextView.setText(section.getLabel());
        previousButton.setText(getPreviousSectionLabel());
        previousButton.setEnabled(hasPreviousSection());
        nextButton.setText(getNextSectionLabel());
        nextButton.setEnabled(hasNextSection());

        tableLayout.removeAllViews();

        int rowId = 0;
        for (DataValue dataValue: section.getExpectedDataValues()) {
            TableRow rowView = (TableRow) inflater.inflate(R.layout.data_viewer_singlerow, null);
            if (rowId % 2 == 0) {
                rowView.setId(1000+rowId);
                rowView.setBackgroundColor(Color.LTGRAY);
            }

            TextView labelTextView = (TextView) rowView.findViewById(R.id.tv_label);
            TextView valueTextView = (TextView) rowView.findViewById(R.id.tv_value);

            String label = dataValue.getActualDataElement().getLabel();
            if (section.hasMultipleCategories()) {
                TextView labelCategoryTextView = (TextView) rowView.findViewById(R.id.tv_category_label);
                labelCategoryTextView.setVisibility(View.VISIBLE);
                labelCategoryTextView.setText(dataValue.getActualCategory().getLabel());
                View barCategoryView = rowView.findViewById(R.id.bar_category_view);
                barCategoryView.setVisibility(View.VISIBLE);
            }
            labelTextView.setText(label);
            valueTextView.setText(dataValue.getDisplayValue());

            tableLayout.addView(rowView);
            rowId++;
        }
    }

}
