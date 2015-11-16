package com.example.karhades_pc.tag_it.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.karhades_pc.tag_it.model.MyTags;
import com.example.karhades_pc.tag_it.model.NfcTag;
import com.example.karhades_pc.tag_it.R;
import com.example.karhades_pc.tag_it.activity.CreateTagActivity;
import com.example.karhades_pc.tag_it.activity.CreateTagPagerActivity;
import com.example.karhades_pc.utils.FontCache;
import com.example.karhades_pc.utils.PictureLoader;
import com.example.karhades_pc.utils.TransitionHelper;

import java.util.ArrayList;

/**
 * Created by Karhades - PC on 4/15/2015.
 */
public class CreateGameFragment extends Fragment {

    /**
     * Request codes, used for startActivityForResult().
     */
    private static final int REQUEST_NEW = 0;
    private static final int REQUEST_EDIT = 1;

    /**
     * Extra key, used for the position value returned from startActivityForResult().
     */
    public static final String EXTRA_POSITION = "com.example.karhades_pc.tag_it.position";

    /**
     * The list with all the NFC tags.
     */
    private ArrayList<NfcTag> nfcTags;

    private RecyclerView recyclerView;
    private FloatingActionButton addActionButton;
    private LinearLayout emptyLinearLayout;

    /**
     * Used for circular reveal.
     */
    private ViewGroup sceneRoot;
    private ViewGroup revealContent;
    private ViewGroup.LayoutParams originalLayoutParams;

    private OnContextualActionBarEnterListener onContextualActionBarEnterListener;

    /**
     * Interface definition for a callback to be invoked when
     * the fragment enters contextual mode.
     */
    public interface OnContextualActionBarEnterListener {
        void onItemLongClicked();

        void onItemClicked(int tagsSelected);
    }

    /**
     * Register a callback to be invoked when the fragment
     * enters contextual mode.
     *
     * @param onContextualActionBarEnterListener The callback that will run.
     */
    public void setOnContextualActionBarEnterListener(OnContextualActionBarEnterListener onContextualActionBarEnterListener) {
        this.onContextualActionBarEnterListener = onContextualActionBarEnterListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nfcTags = MyTags.get(getActivity()).getNfcTags();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NEW) {
            if (resultCode == Activity.RESULT_OK) {
                recyclerView.getAdapter().notifyItemInserted(nfcTags.size());
            }
        } else if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                recyclerView.getAdapter().notifyItemChanged(data.getIntExtra(EXTRA_POSITION, -1));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_game, container, false);

        setupRecyclerView(view);
        setupEmptyView(view);

        return view;
    }

    private void setupRecyclerView(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.create_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        NfcTagAdapter adapter = new NfcTagAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                hideRecyclerViewIfEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);

                hideRecyclerViewIfEmpty();
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // If scrolling down (dy > 0).
                // The faster the scrolling the bigger the dy.
                if (dy > 0) {
                    addActionButton.hide();
                }
                // If scrolling up.
                else if (dy < 0) {
                    addActionButton.show();
                }
            }
        });
    }

    private void setupEmptyView(View view) {
        emptyLinearLayout = (LinearLayout) view.findViewById(R.id.create_empty_linear_layout);
        hideRecyclerViewIfEmpty();
    }

    private void hideRecyclerViewIfEmpty() {
        if (nfcTags.size() == 0) {
            recyclerView.setVisibility(View.INVISIBLE);
            emptyLinearLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLinearLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (addActionButton != null) {
            showActionButton();
            restoreLayoutAfterTransition();
        }
    }

    public void contextDeleteSelectedItems() {
        NfcTagAdapter adapter = (NfcTagAdapter) recyclerView.getAdapter();
        adapter.deleteSelectedItems();

        reorderNfcTags();

        // Inform user.
        Toast.makeText(getActivity(), "Items deleted", Toast.LENGTH_SHORT).show();
    }

    public void contextSelectAll() {
        NfcTagAdapter adapter = (NfcTagAdapter) recyclerView.getAdapter();
        adapter.selectAll();
    }

    public void contextClearSelection() {
        NfcTagAdapter adapter = (NfcTagAdapter) recyclerView.getAdapter();
        adapter.clearSelection();
    }

    public void contextFinish() {
        NfcTagAdapter adapter = (NfcTagAdapter) recyclerView.getAdapter();

        adapter.setSelectionMode(false);
        adapter.clearSelection();
    }

    /**
     * Wraps the data set and creates views for individual items. It's the
     * intermediate that sits between the RecyclerView and the data set.
     */
    private class NfcTagAdapter extends RecyclerView.Adapter<NfcTagHolder> {

        private SparseBooleanArray selectedItems;
        private boolean isSelectionMode = false;

        public NfcTagAdapter() {
            selectedItems = new SparseBooleanArray();
        }

        // Create new views (invoked by the layout manager).
        @Override
        public NfcTagHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            //Log.d("CreateGameFragment", "onCreateViewHolder called");
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_create_game_fragment, viewGroup, false);

            return new NfcTagHolder(view);
        }

        // Replace the contents of a view (invoked by the layout manager).
        @Override
        public void onBindViewHolder(NfcTagHolder nfcTagHolder, int position) {
            NfcTag nfcTag = nfcTags.get(position);

            // Fix the recycling of the holders.
            if (isSelected(position)) {
                nfcTagHolder.itemView.setActivated(true);
            } else {
                nfcTagHolder.itemView.setActivated(false);
            }

            nfcTagHolder.bindRiddle(nfcTag);
        }

        @Override
        public int getItemCount() {
            return nfcTags.size();
        }

        public void setSelectionMode(boolean isSelectableMode) {
            this.isSelectionMode = isSelectableMode;
        }

        public boolean isSelectionMode() {
            return isSelectionMode;
        }

        public void toggleSelection(int position) {
            if (selectedItems.get(position)) {
                selectedItems.delete(position);
            } else {
                selectedItems.put(position, true);
            }
        }

        public boolean isSelected(int position) {
            return selectedItems.get(position);
        }

        public void selectAll() {
            for (int i = 0; i < nfcTags.size(); i++) {
                View view = recyclerView.getChildAt(i);
                if (view instanceof CardView) {
                    selectedItems.put(i, true);
                    view.setActivated(true);
                }
            }
            onContextualActionBarEnterListener.onItemClicked(getSelectionSize());
        }

        public void clearSelection() {
            selectedItems.clear();

            for (int i = 0; i < nfcTags.size(); i++) {
                View view = recyclerView.getChildAt(i);
                if (view instanceof CardView) {
                    view.setActivated(false);
                }
            }
            onContextualActionBarEnterListener.onItemClicked(getSelectionSize());
        }

        public int getSelectionSize() {
            return selectedItems.size();
        }

        public void deleteSelectedItems() {
            for (int i = nfcTags.size(); i >= 0; i--) {
                if (isSelected(i)) {
                    NfcTag nfcTag = nfcTags.get(i);
                    MyTags.get(getActivity()).deleteNfcTag(nfcTag);
                    notifyItemRemoved(i);
                }
            }
        }
    }

    /**
     * Holds all sub views that depend on the current item’s data.
     */
    private class NfcTagHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView titleTextView;
        private TextView difficultyTextView;
        private ImageButton moreImageButton;

        private NfcTag nfcTag;
        private NfcTagAdapter adapter;

        @SuppressWarnings("deprecation")
        public NfcTagHolder(final View view) {
            super(view);

            adapter = (NfcTagAdapter) recyclerView.getAdapter();

            // CardView OnClickListener.
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If Contextual Action Bar is disabled.
                    if (!adapter.isSelectionMode()) {
                        // Start CreateTagPagerActivity.
                        Intent intent = new Intent(getActivity(), CreateTagPagerActivity.class);
                        intent.putExtra(CreateTagFragment.EXTRA_TAG_ID, nfcTag.getTagId());
                        startActivityForResult(intent, REQUEST_EDIT);
                    }
                    // If Contextual Action Bar is enabled.
                    else {
                        // Toggle the selected view.
                        selectItem(view);
                    }
                }
            });
            // CardView OnLongClickListener.
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onContextualActionBarEnterListener.onItemLongClicked();

                    // Enable selection mode.
                    adapter.setSelectionMode(true);

                    // Toggle selected view.
                    selectItem(view);

                    return true;
                }
            });

            // Custom Fonts.
            Typeface typefaceBold = FontCache.get("fonts/capture_it.ttf", getActivity());
            Typeface typefaceNormal = FontCache.get("fonts/amatic_bold.ttf", getActivity());

            imageView = (ImageView) view.findViewById(R.id.row_create_image_view);

            titleTextView = (TextView) view.findViewById(R.id.row_create_title_text_view);
            titleTextView.setTypeface(typefaceBold);

            difficultyTextView = (TextView) view.findViewById(R.id.row_create_difficulty_text_view);
            difficultyTextView.setTypeface(typefaceNormal);
            difficultyTextView.setTextColor(getResources().getColor(R.color.accent));

            moreImageButton = (ImageButton) view.findViewById(R.id.row_create_more_image_button);
            moreImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setupPopupWindow(moreImageButton);
                }
            });
        }

        private void selectItem(View view) {
            // Highlight selected view.
            if (adapter.isSelected(getAdapterPosition())) {
                view.setActivated(false);
            } else {
                view.setActivated(true);
            }

            adapter.toggleSelection(getAdapterPosition());
            onContextualActionBarEnterListener.onItemClicked(adapter.getSelectionSize());
        }

        private void setupPopupWindow(View view) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.popup_window, null);

            final PopupWindow popupWindow = new PopupWindow();
            popupWindow.setContentView(layout);
            popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);

            TextView deleteTextView = (TextView) layout.findViewById(R.id.popup_delete_text_view);
            deleteTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Delete the selected NfcTag.
                    MyTags.get(getActivity()).deleteNfcTag(nfcTag);

                    // Refresh the list.
                    recyclerView.getAdapter().notifyItemRemoved(getAdapterPosition());

                    reorderNfcTags();

                    popupWindow.dismiss();
                }
            });

            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            if (Build.VERSION.SDK_INT >= 21) {
                popupWindow.setElevation(24);
            }
            popupWindow.showAsDropDown(view, 25, -265);
        }

        public void bindRiddle(NfcTag nfcTag) {
            this.nfcTag = nfcTag;

            PictureLoader.loadBitmapWithPicasso(getActivity(), nfcTag.getPictureFilePath(), imageView, null);

            titleTextView.setText(nfcTag.getTitle());
            difficultyTextView.setText(nfcTag.getDifficulty());
        }
    }

    private void reorderNfcTags() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MyTags.get(getActivity()).reorderNfcTags();
                recyclerView.getAdapter().notifyItemRangeChanged(0, MyTags.get(getActivity()).getNfcTags().size());
            }
        }, 1000);
    }

    public void setupFloatingActionButton(View view) {
        addActionButton = (FloatingActionButton) view;
        addActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TransitionHelper.itSupportsTransitions()) {
                    startActivityWithTransition();
                }
                // No transitions.
                else {
                    // Start CreateTagActivity.
                    Intent intent = new Intent(getActivity(), CreateTagActivity.class);
                    startActivityForResult(intent, 0);
                }
            }
        });
    }

    public void setupTransitionViews(ViewGroup sceneRoot, ViewGroup revealContent) {
        this.sceneRoot = sceneRoot;
        this.revealContent = revealContent;
        originalLayoutParams = addActionButton.getLayoutParams();
    }

    @TargetApi(21)
    private void startActivityWithTransition() {
        Transition transition = TransitionInflater.from(getActivity()).inflateTransition(R.transition.changebounds_with_arcmotion);
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                // DO NOTHING.
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                hideActionButton();

                TransitionHelper.circularShow(addActionButton, revealContent, new Runnable() {
                    @Override
                    public void run() {
                        // Start CreateTagActivity.
                        Intent intent = new Intent(getActivity(), CreateTagActivity.class);
                        startActivityForResult(intent, 0);
                    }
                });
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                // DO NOTHING.
            }

            @Override
            public void onTransitionPause(Transition transition) {
                // DO NOTHING.
            }

            @Override
            public void onTransitionResume(Transition transition) {
                // DO NOTHING.
            }
        });

        // View transition.
        TransitionManager.beginDelayedTransition(sceneRoot, transition);

        // Change the action button's gravity from bottom|right to center.
        CoordinatorLayout.LayoutParams newLayoutParams = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        newLayoutParams.gravity = Gravity.CENTER;
        addActionButton.setLayoutParams(newLayoutParams);
    }

    private void showActionButton() {
        if (addActionButton.getScaleX() == 0 && addActionButton.getScaleY() == 0) {
            // Using handler because the setStartDelay method glitches with
            // the show/hide of the action button.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    addActionButton.animate()
                            .scaleX(1)
                            .scaleY(1);
                }
            }, 700);
        }
    }

    private void hideActionButton() {
        addActionButton.setScaleX(0);
        addActionButton.setScaleY(0);
    }

    private void restoreLayoutAfterTransition() {
        if (revealContent.getVisibility() == View.VISIBLE) {
            revealContent.setVisibility(View.INVISIBLE);
            addActionButton.setLayoutParams(originalLayoutParams);
        }
    }
}