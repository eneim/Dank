package me.saket.dank.widgets.swipe;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Views;
import timber.log.Timber;

public class SwipeableLayout extends FrameLayout {

  public static final long ANIMATION_DURATION_FOR_SETTLING_BACK_TO_POSITION = 300;

  private View swipeableChild;
  private SwipeActions swipeActions;
  private SwipeActionIconView actionIconView;
  private SwipeAction activeSwipeAction;
  private boolean swipeDistanceThresholdCrossed;
  private ObjectAnimator translationAnimator;
  private BackgroundDrawable backgroundDrawable;
  private SwipeTriggerRippleDrawable swipeActionTriggerDrawable;
  private boolean swipeEnabled;

  private OnPerformSwipeActionListener onPerformSwipeActionListener;
  private SwipeActionIconProvider swipeActionIconProvider;

  public interface SwipeActionIconProvider {
    /**
     * Called when the visible swipe action changes.
     *
     * @param oldAction The action that was visible before. Null when this is the first action. This
     *                  can be used to figure out any icon animation for <var>newAction</var>'s icon.
     */
    void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction);
  }

  public interface OnPerformSwipeActionListener {
    /**
     * Called when the finger is lifted on an action. Only called when the swipe threshold (presently at
     * 40% of the icon) is crossed.
     */
    void onPerformSwipeAction(SwipeAction action);
  }

  public SwipeableLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    backgroundDrawable = new BackgroundDrawable(new ColorDrawable(Color.TRANSPARENT), new ColorDrawable(Color.DKGRAY));
    backgroundDrawable.setCallback(this);

    setSwipeEnabled(true);
    setSwipeDistanceThresholdCrossed(false);  // Controls the background color's gray tint.

    setWillNotDraw(false);
    swipeActionTriggerDrawable = new SwipeTriggerRippleDrawable();
    swipeActionTriggerDrawable.setCallback(this);
  }

  public void setSwipeActions(SwipeActions actions) {
    swipeActions = actions;
  }

  public void setSwipeActionIconProvider(SwipeActionIconProvider iconProvider) {
    swipeActionIconProvider = iconProvider;
  }

  public void setOnPerformSwipeActionListener(OnPerformSwipeActionListener listener) {
    onPerformSwipeActionListener = listener;
  }

  public void setSwipeEnabled(boolean enabled) {
    swipeEnabled = enabled;

    if (!enabled) {
      activeSwipeAction = null;
      setSwipeDistanceThresholdCrossed(false);
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    if (child instanceof SwipeActionIconView) {
      actionIconView = ((SwipeActionIconView) child);

      // Set a random icon to set the ImageView's initial dimensions.
      Views.executeOnMeasure(actionIconView, () -> {
        actionIconView.setTranslationX(-actionIconView.getWidth());
      });

    } else {
      swipeableChild = child;
    }

    if (getChildCount() > 2) {
      throw new UnsupportedOperationException("SwipeableLayout only supports 2 child Views.");
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (actionIconView == null) {
      throw new IllegalStateException("Action icon is missing");
    }
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (swipeableChild.getTranslationX() != 0f) {
      backgroundDrawable.draw(canvas);
    }
    super.dispatchDraw(canvas);
    swipeActionTriggerDrawable.draw(canvas);
  }

// ======== SWIPE ======== //

  public void setSwipeTranslation(float translationX) {
    if (!isLaidOut()) {
      //throw new IllegalStateException("SwipeableLayout hasn't been measured yet!");
      Timber.w("SwipeableLayout hasn't been measured yet!");
      return;
    }

    // Limit translation to width of swipeable layout.
    int swipeableLayoutWidth = getWidth();
    translationX = Math.max(-swipeableLayoutWidth, Math.min(translationX, swipeableLayoutWidth));

    swipeableChild.setTranslationX(translationX);

    // We want to draw the background drawable in only the visible portion of the background to avoid a redraw.
    boolean swipingFromEndToStart = translationX < 0f;
    if (swipingFromEndToStart) {
      backgroundDrawable.setBounds((int) (getRight() - getLeft() + translationX), 0, getRight() - getLeft(), getBottom() - getTop());
    } else {
      backgroundDrawable.setBounds(0, 0, (int) translationX, getBottom() - getTop());
    }

    if (swipeEnabled) {
      swipeActionTriggerDrawable.setBounds((int) translationX, 0, (int) (swipeableLayoutWidth + translationX), getHeight());

      // Move the icon along with the View being swiped.
      if (swipingFromEndToStart) {
        actionIconView.setTranslationX(swipeableChild.getRight() + translationX);
      } else {
        actionIconView.setTranslationX(translationX - actionIconView.getWidth());
      }

      if (translationX == 0f) {
        backgroundDrawable.animateColorTransition(Color.TRANSPARENT);
        setSwipeDistanceThresholdCrossed(false);
        activeSwipeAction = null;

      } else {
        if (!isSettlingBackToPosition()) {
          SwipeAction swipeAction = swipingFromEndToStart
              ? swipeActions.endActions().findActionAtSwipeDistance(swipeableLayoutWidth, Math.abs(translationX), SwipeDirection.END_TO_START)
              : swipeActions.startActions().findActionAtSwipeDistance(swipeableLayoutWidth, Math.abs(translationX), SwipeDirection.START_TO_END);

          if (activeSwipeAction != swipeAction) {
            SwipeAction oldAction = activeSwipeAction;
            activeSwipeAction = swipeAction;

            // Request an update to the icon.
            swipeActionIconProvider.showSwipeActionIcon(actionIconView, oldAction, swipeAction);

            // Animate the background color transition only if the swipe threshold is passed.
            backgroundDrawable.animateColorTransition(ContextCompat.getColor(getContext(), swipeAction.backgroundColorRes()));
          }

          // Tint the background gray until the swipe threshold is crossed.
          boolean swipeThresholdCrossed = Math.abs(translationX) > actionIconView.getWidth() * 3 / 4;
          setSwipeDistanceThresholdCrossed(swipeThresholdCrossed);
        }
      }
    }
  }

  /**
   * Animate the swipeable child to its original translation.
   */
  public void animateBackToPosition() {
    translationAnimator = ObjectAnimator.ofFloat(this, "swipeTranslation", getSwipeTranslation(), 0f);
    translationAnimator.setDuration(ANIMATION_DURATION_FOR_SETTLING_BACK_TO_POSITION);
    translationAnimator.setInterpolator(Animations.INTERPOLATOR);
    translationAnimator.start();
  }

  /**
   * Whether the swipeable child is being animated back to original translation.
   */
  public boolean isSettlingBackToPosition() {
    return translationAnimator != null && translationAnimator.isRunning();
  }

  public void handleOnRelease() {
    if (hasCrossedSwipeDistanceThreshold()) {
      onPerformSwipeActionListener.onPerformSwipeAction(activeSwipeAction);
    }
  }

  public float getSwipeTranslation() {
    return swipeableChild.getTranslationX();
  }

  private void setSwipeDistanceThresholdCrossed(boolean thresholdCrossed) {
    if (swipeDistanceThresholdCrossed == thresholdCrossed) {
      return;
    }
    swipeDistanceThresholdCrossed = thresholdCrossed;
    backgroundDrawable.animateSwipeThresholdCrossedTransition(thresholdCrossed ? 0 : 255);
  }

  public boolean hasCrossedSwipeDistanceThreshold() {
    return swipeDistanceThresholdCrossed;
  }

  public boolean isSwipeEnabled() {
    return swipeEnabled;
  }

  private static class BackgroundDrawable extends LayerDrawable {
    private ValueAnimator colorTransitionAnimator;
    private ObjectAnimator tintTransitionAnimator;

    /**
     * Creates a new layer drawable with the list of specified layers.
     */
    public BackgroundDrawable(ColorDrawable backgroundColorDrawable, ColorDrawable swipeThresholdIndicatorDrawable) {
      super(new Drawable[] { backgroundColorDrawable, swipeThresholdIndicatorDrawable });
    }

    public ColorDrawable colorDrawable() {
      return (ColorDrawable) getDrawable(0);
    }

    /**
     * Used for indicating whether that the swipe threshold has been crossed.
     */
    public ColorDrawable swipeThresholdTintDrawable() {
      return (ColorDrawable) getDrawable(1);
    }

    /**
     * Animate the background layer's color.
     */
    public void animateColorTransition(@ColorInt int toColor) {
      if (colorTransitionAnimator != null) {
        colorTransitionAnimator.cancel();
      }

      colorTransitionAnimator = ValueAnimator.ofArgb(colorDrawable().getColor(), toColor);
      colorTransitionAnimator.addUpdateListener(animation -> {
        colorDrawable().setColor(((Integer) animation.getAnimatedValue()));
      });
      colorTransitionAnimator.setDuration(200);
      colorTransitionAnimator.setInterpolator(Animations.INTERPOLATOR);
      colorTransitionAnimator.start();
    }

    /**
     * Animate the gray layer's alpha.
     */
    public void animateSwipeThresholdCrossedTransition(@IntRange(from = 0, to = 255) int toAlpha) {
      if (tintTransitionAnimator != null) {
        tintTransitionAnimator.cancel();
      }

      tintTransitionAnimator = ObjectAnimator.ofInt(
          swipeThresholdTintDrawable(),
          "alpha",
          swipeThresholdTintDrawable().getAlpha(),
          toAlpha
      );
      tintTransitionAnimator.setDuration(200);
      tintTransitionAnimator.setInterpolator(Animations.INTERPOLATOR);
      tintTransitionAnimator.start();
    }
  }

// ======== RIPPLE DRAWABLE ======== //

  /**
   * Called from {@link OnPerformSwipeActionListener#onPerformSwipeAction(SwipeAction)}, when a swipe action is performed.
   */
  public void playRippleAnimation(SwipeAction forAction, SwipeTriggerRippleDrawable.RippleType swipeRippleType) {
    int swipeActionColor = ContextCompat.getColor(getContext(), forAction.backgroundColorRes());
    SwipeDirection rippleDirection = swipeActions.startActions().contains(forAction) ? SwipeDirection.START_TO_END : SwipeDirection.END_TO_START;
    swipeActionTriggerDrawable.play(swipeActionColor, rippleDirection, swipeRippleType);
  }

  @Override
  public void invalidateDrawable(Drawable drawable) {
    if (isOurDrawable(drawable)) {
      invalidate();
    } else {
      super.invalidateDrawable(drawable);
    }
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return isOurDrawable(who) || super.verifyDrawable(who);
  }

  private boolean isOurDrawable(Drawable drawable) {
    return drawable == swipeActionTriggerDrawable || drawable == backgroundDrawable;
  }
}
