package ch.swisscom.beaconlocalizationapp.math;

import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 * Models the Multilateration problem. This is a formulation for a nonlinear least
 * squares optimizer.
 *
 * @author Scott Wiedemann
 * https://github.com/lemmingapex/Trilateration
 *
 */
public class MultilaterationFunction implements MultivariateJacobianFunction {

	protected static final double EPSILON = 1E-7;

	/**
	 * Known positions of static nodes
	 */
	protected final double mPositions[][];

	/**
	 * Euclidean mDistances from static nodes to mobile node
	 */
	protected final double mDistances[];

	public MultilaterationFunction(double positions[][], double distances[]) {

		if(positions.length < 2) {
 			throw new IllegalArgumentException("Need at least two mPositions.");
		}

		if(positions.length != distances.length) {
			throw new IllegalArgumentException("The number of mPositions you provided, " + positions.length + ", does not match the number of mDistances, " + distances.length + ".");
		}

		// bound mDistances to strictly positive domain
		for (int i = 0; i < distances.length; i++) {
			distances[i] = Math.max(distances[i], EPSILON);
		}

		int positionDimension = positions[0].length;
		for (int i = 1; i < positions.length; i++) {
			if(positionDimension != positions[i].length) {
				throw new IllegalArgumentException("The dimension of all mPositions should be the same.");
			}
		}

		this.mPositions = positions;
		this.mDistances = distances;
	}

	public final double[] getDistances() {
		return mDistances;
	}

	public final double[][] getPositions() {
		return mPositions;
	}

	/**
	 * Calculate and return Jacobian function Actually return initialized mFunction
	 *
	 * Jacobian matrix, [i][j] at
	 * J[i][0] = delta_[(x0-xi)^2 + (y0-yi)^2 - ri^2]/delta_[x0] at
	 * J[i][1] = delta_[(x0-xi)^2 + (y0-yi)^2 - ri^2]/delta_[y0] partial derivative with respect to the parameters passed to value() method
	 *
	 * @param point for which to calculate the slope
	 * @return Jacobian matrix for point
	 */
	public RealMatrix jacobian(RealVector point) {
		double[] pointArray = point.toArray();

		double[][] jacobian = new double[mDistances.length][pointArray.length];
		for (int i = 0; i < jacobian.length; i++) {
			for (int j = 0; j < pointArray.length; j++) {
				jacobian[i][j] = 2 * pointArray[j] - 2 * mPositions[i][j];
			}
		}

		return new Array2DRowRealMatrix(jacobian);
	}

	@Override
	public Pair<RealVector, RealMatrix> value(RealVector point) {

		// input
		double[] pointArray = point.toArray();

		// output
		double[] resultPoint = new double[this.mDistances.length];

		// compute least squares
		for (int i = 0; i < resultPoint.length; i++) {
			resultPoint[i] = 0.0;
			// calculate sum, add to overall
			for (int j = 0; j < pointArray.length; j++) {
				resultPoint[i] += (pointArray[j] - this.getPositions()[i][j]) * (pointArray[j] - this.getPositions()[i][j]);
			}
			resultPoint[i] -= (this.getDistances()[i]) * (this.getDistances()[i]);
		}

		RealMatrix jacobian = jacobian(point);
		return new Pair<RealVector, RealMatrix>(new ArrayRealVector(resultPoint), jacobian);
	}
}
