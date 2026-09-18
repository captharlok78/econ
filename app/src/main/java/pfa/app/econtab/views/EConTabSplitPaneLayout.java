package pfa.app.econtab.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.Utility;

public class EConTabSplitPaneLayout extends ViewGroup {

	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL = 1;

	private int mOrientation = 0;
	private int mSplitterSize = 12;
	private boolean mSplitterMovable = true;
	private int mSplitterPosition = Integer.MIN_VALUE;
	private int mSplitterMaxPosition = Integer.MIN_VALUE;
	private float mSplitterPositionPercent = 0.5f;
	private float mSplitterPositionMaxPercent = 1f;
	private float mSplitterPositionPercentDefault = 0.5f;

	private Drawable mSplitterDrawable;
	private Drawable mSplitterDrawableButtonDestra;
	private Drawable mSplitterDraggingDrawable;

	private Rect mSplitterRect = new Rect();
	private Rect mSplitterButtonDestra = new Rect();

	private int lastX;
	private int lastY;
	private Rect temp = new Rect();
	private boolean isDragging = false;
	private boolean isTablet = true;

	public EConTabSplitPaneLayout(Context context) {
		super(context);

		mSplitterPositionPercent = 0.5f;
		mSplitterPositionMaxPercent = 1f;
		mSplitterPositionPercentDefault = 0.5f;
		mSplitterDrawable = new PaintDrawable(0x88FFFFFF);
		mSplitterDrawableButtonDestra = new PaintDrawable(0x88FFFFFF);
		mSplitterDraggingDrawable = new PaintDrawable(0x88FFFFFF);

	}

	public EConTabSplitPaneLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		extractAttributes(context, attrs);

	}

	public EConTabSplitPaneLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		extractAttributes(context, attrs);

	}

	private void extractAttributes(Context context, AttributeSet attrs) {

		if (attrs != null && !isInEditMode()) {
			isTablet = Utility.isTablet(getContext());
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SplitPaneLayout);
			mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, 0);
			mSplitterSize = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterSize,
					context.getResources().getDimensionPixelSize(R.dimen.margine));
			mSplitterMovable = a.getBoolean(R.styleable.SplitPaneLayout_splitterMovable, true);
			TypedValue value = a.peekValue(R.styleable.SplitPaneLayout_splitterPosition);
			if (value != null) {
				if (value.type == TypedValue.TYPE_DIMENSION) {
					mSplitterPosition = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterPosition, Integer.MIN_VALUE);
				} else if (value.type == TypedValue.TYPE_FRACTION) {
					mSplitterPositionPercent = a.getFraction(R.styleable.SplitPaneLayout_splitterPosition, 100, 100, 50) * 0.01f;
					try {
						mSplitterPositionMaxPercent = a.getFraction(R.styleable.SplitPaneLayout_splitterMaxPosition, 100, 100, 50) * 0.01f;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						mSplitterPositionMaxPercent = 1f;
					}
					mSplitterPositionPercentDefault = a.getFraction(R.styleable.SplitPaneLayout_splitterPosition, 100, 100, 50) * 0.01f;
				}
			} else {
				mSplitterPosition = Integer.MIN_VALUE;
				mSplitterPositionPercent = 0.5f;
				mSplitterPositionMaxPercent = 1f;
				mSplitterPositionPercentDefault = 0.5f;
			}

			value = a.peekValue(R.styleable.SplitPaneLayout_splitterBackground);
			if (value != null) {
				if (value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING) {
					mSplitterDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterBackground);
					mSplitterDrawableButtonDestra = getResources().getDrawable(R.drawable.frecce);
				} else if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8 || value.type == TypedValue.TYPE_INT_COLOR_ARGB4
						|| value.type == TypedValue.TYPE_INT_COLOR_RGB8 || value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
					mSplitterDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterBackground, 0xFF000000));
				}
			}
			value = a.peekValue(R.styleable.SplitPaneLayout_splitterDraggingBackground);
			if (value != null) {
				if (value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING) {
					mSplitterDraggingDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterDraggingBackground);
				} else if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8 || value.type == TypedValue.TYPE_INT_COLOR_ARGB4
						|| value.type == TypedValue.TYPE_INT_COLOR_RGB8 || value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
					mSplitterDraggingDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterDraggingBackground,
							0x88FFFFFF));
				}
			} else {
				mSplitterDraggingDrawable = new PaintDrawable(0x88FFFFFF);
			}
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		check();

		if (widthSize > 0 && heightSize > 0 && !isInEditMode()) {
			switch (mOrientation) {
			case 0: {
				if (mSplitterPositionPercent > 0.05f) {
					mSplitterDrawableButtonDestra = getResources().getDrawable(R.drawable.frecce_sinistra);
				} else {
					mSplitterDrawableButtonDestra = getResources().getDrawable(R.drawable.frecce);
				}
				if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
					mSplitterPosition = widthSize / 2;
				} else if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent >= 0) {
					mSplitterPosition = (int) (widthSize * mSplitterPositionPercent);
					mSplitterMaxPosition = (int) (widthSize * mSplitterPositionMaxPercent);
				} else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
					mSplitterPositionPercent = (float) mSplitterPosition / (float) widthSize;
				}
				getChildAt(1).measure(MeasureSpec.makeMeasureSpec(mSplitterPosition - (mSplitterSize / 2), MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
				if (isTablet) {
					if (mSplitterPositionPercent > 0.5f) {
						int metaschermo = (int) (widthSize * 0.5f);
						getChildAt(0).measure(
								MeasureSpec.makeMeasureSpec(widthSize - (mSplitterSize / 2) - metaschermo, MeasureSpec.EXACTLY),
								MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
						// if (getChildAt(1) instanceof HorizontalScrollView){
						// ((ViewGroup)getChildAt(1)).getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize -
						// (mSplitterSize / 2) - metaschermo, MeasureSpec.EXACTLY),
						// MeasureSpec.makeMeasureSpec(heightSize,
						// MeasureSpec.EXACTLY));
						// }
					} else {
						getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize - mSplitterPosition, MeasureSpec.EXACTLY),
								MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
						// if (getChildAt(1) instanceof HorizontalScrollView){
						// ((ViewGroup)getChildAt(1)).getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize -
						// mSplitterPosition, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize,
						// MeasureSpec.EXACTLY));
						// }
					}
				} else {
					getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
							MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
				}
				// getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
				// MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));

				break;
			}
			case 1: {
				if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
					mSplitterPosition = heightSize / 2;
				} else if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent >= 0) {
					mSplitterPosition = (int) (heightSize * mSplitterPositionPercent);
					mSplitterMaxPosition = (int) (heightSize * mSplitterPositionMaxPercent);
				} else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
					mSplitterPositionPercent = (float) mSplitterPosition / (float) heightSize;
				}
				getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(mSplitterPosition - (mSplitterSize / 2), MeasureSpec.EXACTLY));
				getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(heightSize - (mSplitterSize / 2) - mSplitterPosition, MeasureSpec.EXACTLY));
				break;
			}
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!isInEditMode()) {
			int w = r - l;
			int h = b - t;
			switch (mOrientation) {
			case 0: {
				getChildAt(1).layout(0, 0, mSplitterPosition, h);
				mSplitterRect.set(mSplitterPosition - (mSplitterSize), 0, mSplitterPosition, h);
				mSplitterButtonDestra.set(mSplitterPosition - getResources().getDimensionPixelSize(R.dimen.altezza_campo), 2,
						mSplitterPosition + getResources().getDimensionPixelSize(R.dimen.altezza_campo), 2 + 5 * getResources()
								.getDimensionPixelSize(R.dimen.margine));
				if (isTablet) {
					getChildAt(0).layout(mSplitterPosition, 0, r, h);
				} else {
					getChildAt(0).layout(0, 0, r, h);
				}
				break;
			}
			case 1: {
				getChildAt(0).layout(0, 0, w, mSplitterPosition - (mSplitterSize / 2));
				mSplitterRect.set(0, mSplitterPosition - (mSplitterSize / 2), w, mSplitterPosition + (mSplitterSize / 2));
				getChildAt(1).layout(0, mSplitterPosition + (mSplitterSize / 2), w, h);
				break;
			}
			}
		}

	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (mSplitterMovable) {
			int x = (int) event.getX();
			int y = (int) event.getY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {

				if (mSplitterButtonDestra.contains(x, y)) {
					return true;
				}

				if ((mSplitterRect.left - 40) <= x && x <= (mSplitterRect.right + 40)) {
					return true;
				}

				break;
			}

			}
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mSplitterMovable) {
			int x = (int) event.getX();
			int y = (int) event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				boolean bottone = false;
				if (mSplitterButtonDestra.contains(x, y)) {

					if (getSplitterPositionPercent() == 0) {
						// setSplitterPositionPercent(mSplitterPositionPercentDefault);

						expand();

					} else {
						// setSplitterPositionPercent(0);
						collapse();
					}

					bottone = true;
				}

				if (bottone == false) {
					int offsetsinistro = 25;
					// if (getSplitterPositionPercent() < 0.2f) {
					// offsetsinistro = 0;
					// }
					if ((mSplitterRect.left - offsetsinistro) <= x && x <= (mSplitterRect.right + 40)) {
						// performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						isDragging = true;
						temp.set(mSplitterRect);
						invalidate(temp);
						lastX = x;
						lastY = y;
					}
				}
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				if (isDragging) {
					switch (mOrientation) {
					case ORIENTATION_HORIZONTAL: {
						if (x <= mSplitterMaxPosition) {
							temp.offset((x - lastX), 0);
							lastX = x;
							lastY = y;
							invalidate();
						}
						break;
					}
					case ORIENTATION_VERTICAL: {
						if (y <= mSplitterMaxPosition) {
							temp.offset(0, (int) (y - lastY));
							lastX = x;
							lastY = y;
							invalidate();
						}
						break;
					}
					}

				}
				break;
			}
			case MotionEvent.ACTION_UP: {
				if (isDragging) {
					isDragging = false;
					switch (mOrientation) {
					case ORIENTATION_HORIZONTAL: {
						mSplitterPosition = x;
						if (x > mSplitterMaxPosition) {
							mSplitterPosition = mSplitterMaxPosition;
						}
						break;
					}
					case ORIENTATION_VERTICAL: {
						mSplitterPosition = y;
						if (y > mSplitterMaxPosition) {
							mSplitterPosition = mSplitterMaxPosition;
						}
						break;
					}
					}
					mSplitterPositionPercent = -1;
					remeasure();
					requestLayout();
				}
				break;
			}
			}
			return true;
		}
		return false;
	}

	public void expand() {
		/*
		 * LeftRightAnim a = new LeftRightAnim(this, mSplitterPositionPercentDefault, true); a.setDuration(200);
		 * 
		 * startAnimation(a);
		 */
		invalidate();

		setSplitterPositionPercent(mSplitterPositionPercentDefault);
		remeasure();
		requestLayout();

	}

	public void collapse() {

		/*
		 * LeftRightAnim a = new LeftRightAnim(this, getSplitterPositionPercent(), false); a.setDuration(200);
		 * 
		 * startAnimation(a);
		 */

		invalidate();

		setSplitterPositionPercent(0);
		remeasure();
		requestLayout();

	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.mSplitterPositionPercent = mSplitterPositionPercent;
		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		setSplitterPositionPercent(ss.mSplitterPositionPercent);
	}

	/**
	 * Convenience for calling own measure method.
	 */
	private void remeasure() {
		measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
	}

	/**
	 * Checks that we have exactly two children.
	 */
	private void check() {
		if (!isInEditMode()) {
			if (getChildCount() != 2) {
				throw new RuntimeException("SplitPaneLayout must have exactly two child views.");
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mSplitterDrawable != null) {
			mSplitterDrawable.setBounds(mSplitterRect);
			mSplitterDrawable.draw(canvas);
		}
		if (mSplitterDrawableButtonDestra != null) {
			mSplitterDrawableButtonDestra.setBounds(mSplitterButtonDestra);
			mSplitterDrawableButtonDestra.draw(canvas);
		}
		if (isDragging) {
			mSplitterDraggingDrawable.setBounds(temp);
			mSplitterDraggingDrawable.draw(canvas);
		}
	}

	/**
	 * Gets the current drawable used for the splitter.
	 * 
	 * @return the drawable used for the splitter
	 */
	public Drawable getSplitterDrawable() {
		return mSplitterDrawable;
	}

	/**
	 * Sets the drawable used for the splitter.
	 * 
	 * @param splitterDrawable
	 *            the desired orientation of the layout
	 */
	public void setSplitterDrawable(Drawable splitterDrawable) {
		mSplitterDrawable = splitterDrawable;
		if (getChildCount() == 2) {
			remeasure();
		}
	}

	/**
	 * Gets the current drawable used for the splitter dragging overlay.
	 * 
	 * @return the drawable used for the splitter
	 */
	public Drawable getSplitterDraggingDrawable() {
		return mSplitterDraggingDrawable;
	}

	/**
	 * Sets the drawable used for the splitter dragging overlay.
	 *
     */
	public void setSplitterDraggingDrawable(Drawable splitterDraggingDrawable) {
		mSplitterDraggingDrawable = splitterDraggingDrawable;
		if (isDragging) {
			invalidate();
		}
	}

	/**
	 * Gets the current orientation of the layout.
	 * 
	 * @return the orientation of the layout
	 */
	public int getOrientation() {
		return mOrientation;
	}

	/**
	 * Sets the orientation of the layout.
	 * 
	 * @param orientation
	 *            the desired orientation of the layout
	 */
	public void setOrientation(int orientation) {
		if (mOrientation != orientation) {
			mOrientation = orientation;
			if (getChildCount() == 2) {
				remeasure();
			}
		}
	}

	/**
	 * Gets the current size of the splitter in pixels.
	 * 
	 * @return the size of the splitter
	 */
	public int getSplitterSize() {
		return mSplitterSize;
	}

	/**
	 * Sets the current size of the splitter in pixels.
	 * 
	 * @param splitterSize
	 *            the desired size of the splitter
	 */
	public void setSplitterSize(int splitterSize) {
		mSplitterSize = splitterSize;
		if (getChildCount() == 2) {
			remeasure();
		}
	}

	/**
	 * Gets whether the splitter is movable by the user.
	 * 
	 * @return whether the splitter is movable
	 */
	public boolean isSplitterMovable() {
		return mSplitterMovable;
	}

	/**
	 * Sets whether the splitter is movable by the user.
	 * 
	 * @param splitterMovable
	 *            whether the splitter is movable
	 */
	public void setSplitterMovable(boolean splitterMovable) {
		mSplitterMovable = splitterMovable;
	}

	/**
	 * Gets the current position of the splitter in pixels.
	 * 
	 * @return the position of the splitter
	 */
	public int getSplitterPosition() {
		return mSplitterPosition;
	}

	/**
	 * Sets the current position of the splitter in pixels.
	 * 
	 * @param position
	 *            the desired position of the splitter
	 */
	public void setSplitterPosition(int position) {
		if (position < 0) {
			position = 0;
		}
		mSplitterPosition = position;
		mSplitterPositionPercent = -1;
		remeasure();
	}

	/**
	 * Gets the current position of the splitter as a percent.
	 * 
	 * @return the position of the splitter
	 */
	public float getSplitterPositionPercent() {
		return mSplitterPositionPercent;
	}

	/**
	 * Sets the current position of the splitter as a percentage of the layout.
	 * 
	 * @param position
	 *            the desired position of the splitter
	 */
	public void setSplitterPositionPercent(float position) {
		if (position < 0) {
			position = 0;
		}
		if (position > 1) {
			position = 1;
		}
		mSplitterPosition = Integer.MIN_VALUE;
		mSplitterPositionPercent = position;
		remeasure();
	}

	/**
	 * Holds important values when we need to save instance state.
	 */
	public static class SavedState extends BaseSavedState {
		float mSplitterPositionPercent;

		SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeFloat(mSplitterPositionPercent);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};

		private SavedState(Parcel in) {
			super(in);
			mSplitterPositionPercent = in.readFloat();
		}
	}

	public class LeftRightAnim extends Animation {
		private final float targetPosition;
		private final boolean right;
		EConTabSplitPaneLayout view = null;

		public LeftRightAnim(EConTabSplitPaneLayout v, float targetPosition, boolean right) {
			this.view = v;
			this.targetPosition = targetPosition;
			this.right = right;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			float newPosition;
			if (right) {
				newPosition = (float) (targetPosition * interpolatedTime);
			} else {
				newPosition = (float) (targetPosition * (1 - interpolatedTime));
			}

			view.setSplitterPositionPercent(newPosition);
			remeasure();
			requestLayout();
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight) {
			super.initialize(width, height, parentWidth, parentHeight);
		}

		@Override
		public boolean willChangeBounds() {
			return true;
		}
	}

}
