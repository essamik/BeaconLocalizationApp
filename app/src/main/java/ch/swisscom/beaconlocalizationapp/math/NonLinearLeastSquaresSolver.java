package ch.swisscom.beaconlocalizationapp.math;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;

/**
 * Solves a Trilateration problem with an instance of a
 * {@link LeastSquaresOptimizer}
 *
 * @author Scott Wiedemann
 * https://github.com/lemmingapex/Trilateration
 *
 */
public class NonLinearLeastSquaresSolver {

	protected final MultilaterationFunction mFunction;
	protected final LeastSquaresOptimizer mLeastSquaresOptimizer;

	protected final static int MAXNUMBEROFITERATIONS = 1000;

	public NonLinearLeastSquaresSolver(MultilaterationFunction function, LeastSquaresOptimizer leastSquaresOptimizer) {
		this.mFunction = function;
		this.mLeastSquaresOptimizer = leastSquaresOptimizer;
	}

	public Optimum solve(double[] target, double[] weights, double[] initialPoint, boolean debugInfo) {
		if (debugInfo) {
			System.out.println("Max Number of Iterations : " + MAXNUMBEROFITERATIONS);
		}

		LeastSquaresProblem leastSquaresProblem = LeastSquaresFactory.create(
				// Function to be optimized
				mFunction,
				// target values at optimal point in least square equation
				// (x0+xi)^2 + (y0+yi)^2 + ri^2 = target[i]
				new ArrayRealVector(target, false), new ArrayRealVector(initialPoint, false), new DiagonalMatrix(weights), null, MAXNUMBEROFITERATIONS, MAXNUMBEROFITERATIONS);

		return mLeastSquaresOptimizer.optimize(leastSquaresProblem);
	}

	public Optimum solve(double[] target, double[] weights, double[] initialPoint) {
		return solve(target, weights, initialPoint, false);
	}

	public Optimum solve(boolean debugInfo) {
		int numberOfPositions = mFunction.getPositions().length;
		int positionDimension = mFunction.getPositions()[0].length;

		double[] initialPoint = new double[positionDimension];
		// initial point, use average of the vertices
		for (int i = 0; i < mFunction.getPositions().length; i++) {
			double[] vertex = mFunction.getPositions()[i];
			for (int j = 0; j < vertex.length; j++) {
				initialPoint[j] += vertex[j];
			}
		}
		for (int j = 0; j < initialPoint.length; j++) {
			initialPoint[j] /= numberOfPositions;
		}

		if (debugInfo) {
			StringBuilder output = new StringBuilder("initialPoint: ");
			for (int i = 0; i < initialPoint.length; i++) {
				output.append(initialPoint[i]).append(" ");
			}
			System.out.println(output.toString());
		}

		double[] target = new double[numberOfPositions];
		double[] distances = mFunction.getDistances();
		double[] weights = new double[target.length];

		for (int i = 0; i < target.length; i++) {
			target[i] = 0.0;
			weights[i] = (1/Math.pow(distances[i], 2));
		}

		return solve(target, weights, initialPoint, debugInfo);
	}

	public Optimum solve() {
		return solve(false);
	}
}
