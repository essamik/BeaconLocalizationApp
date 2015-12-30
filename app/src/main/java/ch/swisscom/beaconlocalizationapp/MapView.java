package ch.swisscom.beaconlocalizationapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class MapView extends ImageView implements View.OnTouchListener {

    interface OnBeaconTouchedListener {
        void onBeaconTouched(BeaconMapObject beaconMapObject);
    }

    private Context mContext;
    private OnBeaconTouchedListener mListener;

    private List<BeaconMapObject> mListMapObjects;
    private Point mCentroidMultilateration;
    private Point mCentroidTrilateration;

    private Paint mCentroidPaint;
    private Paint mUnpairedPaint;
    private Paint mBeaconRangePaint;
    private TextPaint mTextDistancePaint;
    private Bitmap mBeaconBitmap;

    private double mRangedScale;

    private final static int RADIUS_CLOSE_POINT = 50;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mListMapObjects = new ArrayList<>();
        mBeaconBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_beacon);
        setOnTouchListener(this);

        mCentroidPaint = new Paint();
        mCentroidPaint.setColor(Color.RED);
        mCentroidPaint.setAlpha(50);
        mCentroidPaint.setStrokeWidth(3);

        mBeaconRangePaint = new Paint();
        mBeaconRangePaint.setColor(Color.BLUE);
        mBeaconRangePaint.setAlpha(50);
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
                canvas.drawCircle(mapObj.getPosition().x, mapObj.getPosition().y, (float) ( mapObj.getDistance() * mRangedScale), mBeaconRangePaint);
            }

            //Text
            if (mapObj.getDistance() != 0) {
                canvas.drawText(mapObj.getDistance() + "m", mapObj.getPosition().x - iconSize, mapObj.getPosition().y - iconSize, mTextDistancePaint);
            }
        }

        correctCentroidOutOfBox(mCentroidMultilateration, canvas);
        correctCentroidOutOfBox(mCentroidTrilateration, canvas);

        if (mCentroidMultilateration != null) {
            canvas.drawCircle(mCentroidMultilateration.x, mCentroidMultilateration.y, (int) mRangedScale/2, mCentroidPaint);
        }

        if (mCentroidTrilateration != null) {
            Paint paint = new Paint(mCentroidPaint);
            paint.setColor(Color.YELLOW);
            paint.setAlpha(50);
            canvas.drawCircle(mCentroidTrilateration.x, mCentroidTrilateration.y, (int) mRangedScale/2, paint);
        }
    }

    private void correctCentroidOutOfBox(Point centroid, Canvas canvas) {
        if (centroid != null) {
            Point maxPt = getMaxPoint();
            Point minPt = getMinPoint();
            if (centroid.x < minPt.x) {
                centroid.x = minPt.x;
            }
            if (centroid.y < minPt.y) {
                centroid.y = minPt.y;
            }
            if (centroid.x > maxPt.x) {
                centroid.x = maxPt.x;
            }
            if (centroid.y > maxPt.y) {
                centroid.y = maxPt.y;
            }
        }
    }

    public void setCentroidMultilateration(Point centroid) {
        mCentroidMultilateration = centroid;
    }

    public void setCentroidTrilateration(Point centroid) {
        mCentroidTrilateration = centroid;
    }


    public void setBeaconsOnMap(List<BeaconMapObject> mapBeacons) {
        mListMapObjects = mapBeacons;
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
                onBeaconTouched(pt);
                invalidate();
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
        BeaconMapObject foundBeacon = null;
        for (BeaconMapObject myMapBeacon : mListMapObjects) {
            if (myMapBeacon.getPosition().x == position.x && myMapBeacon.getPosition().y == position.y) {
                myMapBeacon.setDistance(distance);
                break;
            }
        }
    }

    private Point getMaxPoint() {
        Point maxPt = new Point(mListMapObjects.get(0).getPosition());

        for (BeaconMapObject beacon : mListMapObjects) {
            if (beacon.getPosition().x > maxPt.x) maxPt.x = beacon.getPosition().x;
            if (beacon.getPosition().y > maxPt.y) maxPt.y = beacon.getPosition().y;
        }

        return maxPt;
    }

    private Point getMinPoint() {
        Point minPt = new Point(mListMapObjects.get(0).getPosition());

        for (BeaconMapObject beacon : mListMapObjects) {
            if (beacon.getPosition().x < minPt.x) minPt.x = beacon.getPosition().x;
            if (beacon.getPosition().y < minPt.y) minPt.y = beacon.getPosition().y;
        }

        return minPt;
    }
}