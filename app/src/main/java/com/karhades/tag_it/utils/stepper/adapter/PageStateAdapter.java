package com.karhades.tag_it.utils.stepper.adapter;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.karhades.tag_it.utils.stepper.AbstractStep;
import com.karhades.tag_it.utils.stepper.interfaces.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Francesco Cannizzaro (fcannizzaro).
 */

public class PageStateAdapter extends FragmentStatePagerAdapter implements Pageable {

    private ArrayList<AbstractStep> fragments = new ArrayList<>();

    public PageStateAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public void add(AbstractStep fragment) {
        fragments.add(fragment);
        notifyDataSetChanged();
    }

    @Override
    public void set(List<AbstractStep> fragments) {
        this.fragments.clear();
        this.fragments.addAll(fragments);
        notifyDataSetChanged();
    }

    @Override
    public AbstractStep getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

}
