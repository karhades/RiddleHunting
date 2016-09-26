package com.karhades.tag_it.utils.stepper.interfaces;

/**
 * @author Francesco Cannizzaro (fcannizzaro).
 */
public interface Nextable {

    boolean nextIf();

    boolean isOptional();

    void onStepVisible();

    void onNext();

    void onPrevious();

    String optional();

    String error();

}
