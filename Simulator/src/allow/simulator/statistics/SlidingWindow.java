package allow.simulator.statistics;

public class SlidingWindow {

	private int windowSize;
	private double windowInv;
	private int index;
	private double values[];
	private double mean;

	public SlidingWindow(int windowSize) {
		this.windowSize = windowSize;
		windowInv = (double) 1.0 / windowSize;
		index = 0;
		values = new double[windowSize];
		mean = 0.0;
	}
	
	public void addValue(double value) {
		double oldValue = values[index];
		values[index] = value;
		mean += windowInv * (value - oldValue);
		index++;
		
		if (index == windowSize) index = 0;
	}
	
	public double getMean() {
		return mean;
		//double acc = 0.0;
		//acc = Arrays.stream(values).sum();
		//for (int i = 0; i < values.length; i++) acc += values[i];
		//return windowInv * acc; 
	}
	
	public void reset() {
		for (int i = 0; i < values.length; i++) values[i] = 0;
		index = 0;
		mean = 0.0;
	}
}
