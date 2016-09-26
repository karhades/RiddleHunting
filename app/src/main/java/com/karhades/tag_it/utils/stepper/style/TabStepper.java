package com.karhades.tag_it.utils.stepper.style;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.karhades.tag_it.R;
import com.karhades.tag_it.utils.stepper.AbstractStep;
import com.karhades.tag_it.utils.stepper.util.LinearityChecker;

/**
 * @author Francesco Cannizzaro (fcannizzaro).
 */
public class TabStepper extends BasePager implements View.OnClickListener {

    protected TextView mError;

    // attributes
    int unselected = Color.parseColor("#9e9e9e");

    // views
    private HorizontalScrollView mTabs;
    private LinearLayout mStepTabs;
    private boolean mLinear;
    private boolean showPrevButton = false;
    private boolean disabledTouch = false;
    private boolean mTabAlternative;
    private ViewSwitcher mSwitch;
    private LinearityChecker mLinearity;
    private Button mContinue;
    private TextView mPreviousButton;

    protected void setLinear(boolean mLinear) {
        this.mLinear = mLinear;
    }

    protected void setDisabledTouch() {
        this.disabledTouch = true;
    }

    protected void setPreviousVisible() {
        this.showPrevButton = true;
    }

    protected void setAlternativeTab(boolean mTabAlternative) {
        this.mTabAlternative = mTabAlternative;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();

        if (getSupportActionBar() != null)
            getSupportActionBar().setElevation(0);

        setContentView(com.karhades.tag_it.R.layout.stepper_style_horizontal_tabs);

        init();

        mTabs = (HorizontalScrollView) findViewById(R.id.stepper_steps);
        mStepTabs = (LinearLayout) mTabs.findViewById(R.id.stepper_stepTabs);
        mSwitch = (ViewSwitcher) findViewById(R.id.stepper_stepSwitcher);
        mError = (TextView) findViewById(R.id.stepper_stepError);
        mPreviousButton = (TextView) findViewById(R.id.stepper_stepPrev);

        mContinue = (Button) findViewById(R.id.stepper_continueButton);
//        mContinue.setTextColor(primaryColor);
        mContinue.setOnClickListener(this);

        mSwitch.setDisplayedChild(0);
        mSwitch.setInAnimation(this, R.anim.in_from_bottom);
        mSwitch.setOutAnimation(this, R.anim.out_to_bottom);

        mLinearity = new LinearityChecker(mSteps.total());

        if (!showPrevButton)
            mPreviousButton.setVisibility(View.GONE);

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevious();
            }
        });

        onUpdate();
    }

    @Override
    public void onUpdate() {

        int i = 0;

        if (mStepTabs.getChildCount() == 0) {
            while (i < mSteps.total()) {
                AbstractStep step = mSteps.get(i);
                mStepTabs.addView(createStepTab(i++, step.name(), step.isOptional(), step.optional()));
            }
        }

        int size = mStepTabs.getChildCount();

        for (i = 0; i < size; i++) {

            View view = mStepTabs.getChildAt(i);

            boolean done = mLinearity.isDone(i);
            View doneIcon = view.findViewById(R.id.done);
            View stepIcon = view.findViewById(R.id.step);
            View errorIcon = view.findViewById(R.id.error);

            doneIcon.setVisibility(done ? View.VISIBLE : View.GONE);
            stepIcon.setVisibility(!done ? View.VISIBLE : View.GONE);
            errorIcon.setVisibility(View.GONE);
            color(done ? doneIcon : stepIcon, i == mSteps.current() || done);

            TextView stepTitle = (TextView) view.findViewById(R.id.title);
            stepTitle.setTypeface(i == mSteps.current() || done ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            view.findViewById(R.id.title).setAlpha(i == mSteps.current() || done ? 1 : 0.54f);
            stepTitle.setTextColor(getResources().getColor(android.R.color.white));

            mPreviousButton.setVisibility(showPrevButton && mSteps.current() > 0 ? View.VISIBLE : View.GONE);

        }

        if (mSteps.current() == mSteps.total() - 1)
            mContinue.setText(com.github.fcannizzaro.materialstepper.R.string.ms_end);
        else
            mContinue.setText(com.github.fcannizzaro.materialstepper.R.string.ms_continue);

    }

    private boolean updateDoneCurrent() {
        if (mSteps.getCurrent().nextIf()) {
            mLinearity.setDone(mSteps.current() + 1);
            return true;
        }
        return mSteps.getCurrent().isOptional();
    }

    private View createStepTab(final int position, String title, boolean isOptional, String optionalStr) {
        View view = getLayoutInflater().inflate(mTabAlternative ? R.layout.step_tab_alternative : R.layout.stepper_step_tab, mStepTabs, false);
        ((TextView) view.findViewById(com.github.fcannizzaro.materialstepper.R.id.step)).setText(String.valueOf(position + 1));

        if (isOptional) {
            view.findViewById(R.id.optional).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.optional)).setText(optionalStr);
        }

        if (position == mSteps.total() - 1)
            view.findViewById(R.id.divider).setVisibility(View.GONE);

        ((TextView) view.findViewById(R.id.title)).setText(title);


        if (!disabledTouch)
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    boolean optional = mSteps.getCurrent().isOptional();

                    if (position != mSteps.current())
                        updateDoneCurrent();

                    if (!mLinear || optional || mLinearity.beforeDone(position)) {
                        mSteps.current(position);
                        updateScrolling(position);
                    } else
                        onError();

                    onUpdate();
                }
            });

        return view;
    }

    private void color(View view, boolean selected) {
        Drawable d = view.getBackground();
        d.setColorFilter(new PorterDuffColorFilter(selected ? primaryColor : unselected, PorterDuff.Mode.SRC_ATOP));
    }

    private void updateScrolling(int newPosition) {
        View tab = mStepTabs.getChildAt(mSteps.current());
        boolean isNear = mSteps.current() == newPosition - 1 || mSteps.current() == newPosition + 1;
        mPager.setCurrentItem(mSteps.current(), isNear);
        mTabs.smoothScrollTo(tab.getLeft() - 20, 0);
        onUpdate();
    }

    @Override
    public void onError() {

//        mError.setText(Html.fromHtml(mSteps.getCurrent().error()));
//
//        if (mSwitch.getDisplayedChild() == 0)
//            mSwitch.setDisplayedChild(1);
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mSwitch.getDisplayedChild() == 1) mSwitch.setDisplayedChild(0);
//            }
//        }, getErrorTimeout() + 300);

        int position = mSteps.current();

        View view = mStepTabs.getChildAt(position);

        TextView stepIcon = (TextView) view.findViewById(R.id.step);
        ImageView errorIcon = (ImageView) view.findViewById(R.id.error);
        TextView stepTitle = (TextView) view.findViewById(R.id.title);

        stepIcon.setVisibility(View.GONE);
        errorIcon.setVisibility(View.VISIBLE);

        stepTitle.setTextColor(getResources().getColor(R.color.red));

        Snackbar.make(findViewById(R.id.stepper_root), mSteps.getCurrent().error(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onPrevious() {
        super.onPrevious();
        updateScrolling(mSteps.current() - 1);
    }

    @Override
    public void onClick(View view) {
        if (updateDoneCurrent()) {
            onNext();
            updateScrolling(mSteps.current() + 1);
        } else
            onError();
    }

    @Deprecated
    protected void disabledTouch() {
        this.disabledTouch = true;
    }

    @Deprecated
    protected void showPreviousButton() {
        this.showPrevButton = true;
    }

}
