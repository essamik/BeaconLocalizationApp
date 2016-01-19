package ch.swisscom.beaconlocalizationapp.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import ch.swisscom.beaconlocalizationapp.Constants;
import ch.swisscom.beaconlocalizationapp.R;
import ch.swisscom.beaconlocalizationapp.model.BeaconMapObject;

public class MapView extends ImageView implements View.OnTouchListener {


    interface OnBeaconTouchedListener {
        void onBeaconTouched(BeaconMapObject beaconMapObject);
    }

    private Context mContext;
    private OnBeaconTouchedListener mListener;
    private boolean mUserPositionMode;

    private List<BeaconMapObject> mListMapObjects;
    private List<Point> mListCentroid;
    private Point mCentroidMultilateration;
    private Point mCentroidTrilateration;
    private Point mUserPosition;

    private Paint mCentroidPaint;
    private Paint mUnpairedPaint;
    private Paint mBeaconRangePaint;
    private TextPaint mTextDistancePaint;
    private Bitmap mBeaconBitmap;

    private double mRangedScale;

    private final static int RADIUS_CLOSE_POINT = 50;
    private final static int RANGE_ALPHA_NORMAL = 40;
    private final static int RANGE_ALPHA_BIG = 25;


    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mListMapObjects = new ArrayList<>();
        mListCentroid = new ArrayList<>();
        mBeaconBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_beacon);
        super.setOnTouchListener(this);

        mCentroidPaint = new Paint();
        mCentroidPaint.setColor(Color.RED);
        mCentroidPaint.setAlpha(255);
        mCentroidPaint.setStrokeWidth(3);

        mBeaconRangePaint = new Paint();
        mBeaconRangePaint.setColor(Color.BLUE);
        mBeaconRangePaint.setAlpha(RANGE_ALPHA_NORMAL);
        mBeaconRangePaint.setStrokeWidth(3);

        mUnpairedPaint = new Paint();
        mUnpairedPaint.setAlpha(100);

        mTextDistancePaint = new TextPaint();
        int size = 12;
        int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, getResources().getDisplayMetrics());

        mTextDistancePaint.setTextSize(pixels);
        mTextDistancePaint.setColor(Color.BLACK);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mRangedScale = Double.valueOf(settings.getString(Constants.PREFS_SCALE, "10"));
    }

    public void setListener(OnBeaconTouchedListener listener) {
        if (listener != null) {
            mListener = listener;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        int iconSize = mBeaconBitmap.getHeight()/2;

        for (BeaconMapObject mapObj : mListMapObjects) {
            canvas.drawBitmap(mBeaconBitmap, mapObj.getPosition().x - iconSize, mapObj.getPosition().y - iconSize, mapObj.isPaired() ? new Paint() : mUnpairedPaint);
            if (mapObj.getDistance() != 0) {
                mBeaconRangePaint.setColor(mCentroidMultilateration == null ? Color.RED : Color.BLUE);
                mBeaconRangePaint.setAlpha(mapObj.getDistance() > 10 ? RANGE_ALPHA_BIG : RANGE_ALPHA_NORMAL);
                canvas.drawCircle(mapObj.getPosition().x, mapObj.getPosition().y, (float) (mapObj.getDistance() * mRangedScale), mBeaconRangePaint);
                //Text
                canvas.drawText(mapObj.getDistance() + "m", mapObj.getPosition().x - iconSize, mapObj.getPosition().y - iconSize, mTextDistancePaint);
            }
        }

        if (mCentroidMultilateration != null) {
            mCentroidPaint.setAlpha(255);
            canvas.drawCircle(mCentroidMultilateration.x, mCentroidMultilateration.y, (int) mRangedScale/4, mCentroidPaint);
            if (mUserPositionMode) {
                for (Point pt : mListCentroid) {
                    mCentroidPaint.setAlpha(50);
                    canvas.drawCircle(pt.x, pt.y, (int) mRangedScale / 4, mCentroidPaint);
                }
            }
        }

        if (mCentroidTrilateration != null) {
            Paint paint = new Paint(mCentroidPaint);
            paint.setColor(Color.YELLOW);
            paint.setAlpha(255);
            canvas.drawCircle(mCentroidTrilateration.x, mCentroidTrilateration.y, (int) mRangedScale/4, paint);
        }

        //Draw user position
        if (mUserPosition != null) {
            Paint paint = new Paint(mCentroidPaint);
            paint.setColor(Color.GREEN);
            paint.setAlpha(255);
            canvas.drawCircle(mUserPosition.x, mUserPosition.y, (int) mRangedScale/5, paint);
        }
    }

    public void setCentroidMultilateration(Point centroid) {
        if (mUserPositionMode) mListCentroid.add(centroid);
        mCentroidMultilateration = centroid;
    }

    public void setCentroidTrilateration(Point centroid) {
        mCentroidTrilateration = centroid;
    }

    public void addBeacon(BeaconMapObject mapBeacon) {
        mListMapObjects.add(mapBeacon);
        invalidate();
    }

    private void onBeaconTouched(Point touchedPt) {
        for (BeaconMapObject myMapBeacon : mListMapObjects) {
            if (pointIsInClosureRadius(touchedPt, myMapBeacon.getPosition())) {
                mListener.onBeaconTouched(myMapBeacon);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Point pt = new Point((int) event.getX(), (int) event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mUserPositionMode) {
                    onBeaconTouched(pt);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mUserPositionMode) {
                    mUserPosition = pt;
                    invalidate();
                }
                break;
        }
        return true;
    }

    /**
     * Check if the given touched point is close to the given beacon position
     * @param touchedPoint : The coordinate of the touched point
     * @param touchedPoint : The coordinate of a beacon on the map
     * @return : True if the coordinate is in the radius of the beacon, false else.
     */
    public boolean pointIsInClosureRadius(Point touchedPoint, Point beaconPosition) {
        boolean isInRadius = false;

        int absDiffX = Math.abs(touchedPoint.x - beaconPosition.x);
        int absDiffY = Math.abs(touchedPoint.y - beaconPosition.y);

        float radius = RADIUS_CLOSE_POINT;
        if ((absDiffX >= 0 && absDiffX <= radius) && (absDiffY >= 0 && absDiffY <= radius)) {
            isInRadius = true;
        }
        return isInRadius;
    }

    public void clearMap() {
        mListMapObjects = new ArrayList<>();
        invalidate();
    }

    public void updateBeaconDistanceByPosition(Point position, double distance) {
        for (BeaconMapObject myMapBeacon : mListMapObjects) {
            if (myMapBeacon.getPosition().x == position.x && myMapBeacon.getPosition().y == position.y) {
                myMapBeacon.setDistance(distance);
                break;
            }
        }
    }

    public void setAddUserPositionMode(boolean userPositionMode) {
        mUserPositionMode = userPositionMode;
        if (!mUserPositionMode) mListCentroid = new ArrayList<>();
    }

    public double gerErrorDelta() {
        if (mUserPositionMode && mUserPosition != null && mCentroidMultilateration != null) {
            Point deltaPt = new Point(Math.abs(mUserPosition.x - mCentroidMultilateration.x), Math.abs(mUserPosition.y - mCentroidMultilateration.y));
            double deltaInPx = Math.sqrt(Math.pow(deltaPt.x, 2) + Math.pow(deltaPt.y, 2));
            return deltaInPx / mRangedScale;

        } else return -1;
    }
}